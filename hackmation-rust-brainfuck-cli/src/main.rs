use std::env;
use std::fs;
use std::io::{self, Write};
use std::process::ExitCode;

mod interpreter;

use interpreter::run;

fn main() -> ExitCode {
    let args: Vec<String> = env::args().skip(1).collect();
    let source = match read_source(&args) {
        Ok(source) => source,
        Err(message) => {
            eprintln!("error: {message}");
            return ExitCode::FAILURE;
        }
    };

    let stdin = io::stdin();
    let stdout = io::stdout();
    let result = run(&source, &mut stdin.lock(), &mut stdout.lock());
    io::stdout().flush().ok();

    match result {
        Ok(()) => ExitCode::SUCCESS,
        Err(error) => {
            eprintln!("error: {error}");
            ExitCode::FAILURE
        }
    }
}

fn read_source(args: &[String]) -> Result<String, String> {
    match args {
        [flag, path] if flag == "-f" => {
            fs::read_to_string(path).map_err(|e| format!("cannot read {path}: {e}"))
        }
        [source] => Ok(source.clone()),
        _ => Err("usage: bf '<program>' | bf -f <file>".to_string()),
    }
}
