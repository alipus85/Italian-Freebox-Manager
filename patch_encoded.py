import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    content = content.replace('@Path("path", encoded = true)', '@Path("path")')
    content = content.replace('@Path("path", encoded = false)', '@Path("path")')
    
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/data/api/FreeboxApi.kt")
