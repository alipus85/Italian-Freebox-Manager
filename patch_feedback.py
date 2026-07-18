import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    content = content.replace('feedback = if (result.isSuccess) "Download completato" else "Errore download"', 'feedback = if (result.isSuccess) "Download completato in Download/" else "Errore download"')
    
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/ui/FreeboxViewModel.kt")
