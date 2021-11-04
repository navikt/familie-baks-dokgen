#!/usr/bin/env python
import argparse
import time
from re import compile
from pathlib import Path
from watchdog.observers import Observer
from watchdog.events import RegexMatchingEventHandler


def parse_args():
    parser = argparse.ArgumentParser(description="Compiles handlebars templates for dokgen")
    parser.add_argument(
        "--no-watch",
        dest="should_watch",
        const=False,
        default=True,
        action="store_const",
        help="Don't watch for filesystem changes, only compile initially found templates",
    )
    return parser.parse_args()


def compile_hbs(contents: str) -> str:
    regex = compile(r"^\s+")
    lines = contents.split("\n")
    return "\n".join([regex.sub("", line) for line in lines])


def find_paths_from_event(event_src_path: str) -> tuple[Path, Path]:
    path = Path(event_src_path)
    parent_dir = path.parent.parent
    file = path.name
    output = parent_dir / file
    return path, output


def read_compile_and_write_template(path: Path, write_to: Path):
    contents = path.read_text()
    compiled = compile_hbs(contents)
    write_to.write_text(compiled)
    print(
        f"Compiled \u001b[37;1m{path}\u001b[0m, output: \u001b[37;1m{write_to}\u001b[0m"
    )


def on_modified(event):
    modified_file, output = find_paths_from_event(event.src_path)
    read_compile_and_write_template(modified_file, output)


def on_deleted(event):
    deleted_file, output = find_paths_from_event(event.src_path)
    output.unlink(missing_ok=True)
    print(
        f"Deleted file \u001b[37;1m{output}\u001b[0m as uncompiled template \u001b[37;1m{deleted_file}\u001b[0m was removed"
    )


def main():
    args = parse_args()
    patterns = [r".*/content/templates/.+/uncompiled/.+\.hbs"]
    ignore_patterns = ["^.*~$"]
    path = "."

    event_handler = RegexMatchingEventHandler(patterns, ignore_patterns)
    event_handler.on_modified = on_modified
    event_handler.on_deleted = on_deleted

    observer = Observer()
    observer.schedule(event_handler, path, recursive=True)

    print("Compiling all found uncompiled files...")
    for pattern in patterns:
        regex = compile(pattern)
        for file in Path(path).rglob("*.hbs"):
            if regex.match(str(file.resolve())):
                read_compile_and_write_template(file, file.parent.parent / file.name)

    if not args.should_watch:
        return

    print("")
    print("Watching for changes to uncompiled files...")
    observer.start()

    try:
        while True:
            time.sleep(0.05)
    except KeyboardInterrupt:
        observer.stop()
        observer.join()


if __name__ == "__main__":
    main()
