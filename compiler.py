#!/usr/bin/env python
import time
from re import compile
from pathlib import Path
from watchdog.observers import Observer
from watchdog.events import RegexMatchingEventHandler


def compile_hbs(contents: str) -> str:
    regex = compile(r"^\s+")
    lines = contents.split('\n')
    return "\n".join([regex.sub("", line) for line in lines])


def find_paths_from_event(event_src_path: str) -> tuple[Path, Path]:
    path = Path(event_src_path)
    parent_dir = path.parent.parent
    file = path.name
    output = parent_dir / file
    return path, output


def on_modified(event):
    modified_file, output = find_paths_from_event(event.src_path)

    contents = modified_file.read_text()
    compiled = compile_hbs(contents)
    output.write_text(compiled)
    print(f"Compiled \u001b[37;1m{modified_file}\u001b[0m, output: \u001b[37;1m{output}\u001b[0m")


def on_deleted(event):
    deleted_file, output = find_paths_from_event(event.src_path)
    output.unlink(missing_ok=True)
    print(f'Deleted file \u001b[37;1m{output}\u001b[0m as uncompiled template \u001b[37;1m{deleted_file}\u001b[0m was removed')


if __name__ == "__main__":
    patterns = [r".*/content/templates/.+/uncompiled/.+\.hbs"]
    ignore_patterns = ["^.*~$"]
    path = "."

    event_handler = RegexMatchingEventHandler(patterns, ignore_patterns)
    event_handler.on_modified = on_modified
    event_handler.on_deleted = on_deleted

    observer = Observer()
    observer.schedule(event_handler, path, recursive=True)

    print("Watching for changes to uncompiled files...")
    observer.start()

    try:
        while True:
            time.sleep(0.05)
    except KeyboardInterrupt:
        observer.stop()
        observer.join()