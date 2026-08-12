use std::collections::HashMap;
use std::fmt;
use std::io::{Read, Write};

pub const TAPE_SIZE: usize = 30_000;

#[derive(Debug)]
pub enum BfError {
    UnbalancedBracket { position: usize },
    PointerOutOfBounds { position: usize },
    Io(std::io::Error),
}

impl fmt::Display for BfError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            BfError::UnbalancedBracket { position } => {
                write!(f, "unbalanced bracket at byte {position}")
            }
            BfError::PointerOutOfBounds { position } => {
                write!(f, "pointer out of bounds at byte {position}")
            }
            BfError::Io(e) => write!(f, "io error: {e}"),
        }
    }
}

impl From<std::io::Error> for BfError {
    fn from(e: std::io::Error) -> Self {
        BfError::Io(e)
    }
}

/// Interprets Brainfuck `source`, reading `,` bytes from `input` and writing
/// `.` bytes to `output`. Cells are wrapping u8 on a 30000-cell tape.
/// On EOF, `,` sets the current cell to 0.
/// Fails on unbalanced brackets (before execution) and on moving the pointer
/// outside the tape.
pub fn run<R: Read, W: Write>(source: &str, input: &mut R, output: &mut W) -> Result<(), BfError> {
    let code: Vec<(usize, u8)> = source
        .bytes()
        .enumerate()
        .filter(|(_, b)| matches!(b, b'+' | b'-' | b'<' | b'>' | b'[' | b']' | b'.' | b','))
        .collect();
    let jumps = match_brackets(&code)?;

    let mut tape = [0u8; TAPE_SIZE];
    let mut ptr = 0usize;
    let mut pc = 0usize;

    while pc < code.len() {
        let (position, op) = code[pc];
        match op {
            b'+' => tape[ptr] = tape[ptr].wrapping_add(1),
            b'-' => tape[ptr] = tape[ptr].wrapping_sub(1),
            b'>' => {
                ptr += 1;
                if ptr >= TAPE_SIZE {
                    return Err(BfError::PointerOutOfBounds { position });
                }
            }
            b'<' => {
                if ptr == 0 {
                    return Err(BfError::PointerOutOfBounds { position });
                }
                ptr -= 1;
            }
            b'.' => output.write_all(&[tape[ptr]])?,
            b',' => {
                let mut byte = [0u8; 1];
                tape[ptr] = match input.read(&mut byte)? {
                    0 => 0,
                    _ => byte[0],
                };
            }
            b'[' => {
                if tape[ptr] == 0 {
                    pc = jumps[&pc];
                }
            }
            b']' => {
                if tape[ptr] != 0 {
                    pc = jumps[&pc];
                }
            }
            _ => unreachable!(),
        }
        pc += 1;
    }
    Ok(())
}

fn match_brackets(code: &[(usize, u8)]) -> Result<HashMap<usize, usize>, BfError> {
    let mut jumps = HashMap::new();
    let mut stack = Vec::new();
    for (pc, &(position, op)) in code.iter().enumerate() {
        match op {
            b'[' => stack.push((pc, position)),
            b']' => {
                let (open, _) = stack
                    .pop()
                    .ok_or(BfError::UnbalancedBracket { position })?;
                jumps.insert(open, pc);
                jumps.insert(pc, open);
            }
            _ => {}
        }
    }
    if let Some(&(_, position)) = stack.last() {
        return Err(BfError::UnbalancedBracket { position });
    }
    Ok(jumps)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn run_bf(source: &str, input: &[u8]) -> Result<Vec<u8>, BfError> {
        let mut reader = input;
        let mut output = Vec::new();
        run(source, &mut reader, &mut output)?;
        Ok(output)
    }

    #[test]
    fn hello_world() {
        let source = "++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]\
                      >>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++.";
        let output = run_bf(source, &[]).unwrap();
        assert_eq!(output, b"Hello World!\n");
    }

    #[test]
    fn echo_until_eof() {
        let output = run_bf(",[.,]", b"hi there").unwrap();
        assert_eq!(output, b"hi there");
    }

    #[test]
    fn comma_on_eof_sets_zero() {
        // Reads twice past EOF; second read must leave 0 so the loop exits.
        let output = run_bf(",[.,]", b"A").unwrap();
        assert_eq!(output, b"A");
    }

    #[test]
    fn nested_loops_multiplication() {
        // 7 * 7 = 49 = ASCII '1'
        let output = run_bf("+++++++[>+++++++<-]>.", &[]).unwrap();
        assert_eq!(output, b"1");
    }

    #[test]
    fn cell_wraps_around() {
        let output = run_bf("-.", &[]).unwrap();
        assert_eq!(output, &[255]);
    }

    #[test]
    fn unbalanced_open_bracket_fails() {
        assert!(matches!(
            run_bf("[[", &[]),
            Err(BfError::UnbalancedBracket { .. })
        ));
    }

    #[test]
    fn unbalanced_close_bracket_fails() {
        assert!(matches!(
            run_bf("+]", &[]),
            Err(BfError::UnbalancedBracket { .. })
        ));
    }

    #[test]
    fn pointer_underflow_fails() {
        assert!(matches!(
            run_bf("<", &[]),
            Err(BfError::PointerOutOfBounds { .. })
        ));
    }

    #[test]
    fn non_command_bytes_are_comments() {
        let output = run_bf("comment! ++ more . text", &[]).unwrap();
        assert_eq!(output, &[2]);
    }
}
