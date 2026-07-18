import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Revert URL_SAFE back to standard NO_WRAP
    content = content.replace("android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP", "android.util.Base64.NO_WRAP")
    
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/data/repository/FreeboxRepository.kt")
