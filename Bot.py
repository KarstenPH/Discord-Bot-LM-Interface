import os
import json
import requests
import sseclient  # pip install sseclient-py
from datetime import datetime


def is_windows():
    return os.name == "nt"


if not is_windows():

    print('''
Sorry, but this script can't run on your machine!
This code is for Windows based operating systems only! (for now)\n\n
    (i) For Linux users, Wine might work, however because the file systems are very different expect a ton of bugs.
        ''')
    exit()


scriptdir = os.path.dirname(os.path.abspath(__file__))
print(f"Script Directory: {scriptdir}")
url = "http://127.0.0.1:5000/v1/completions"

headers = {
    "Content-Type": "application/json"
}


def fallback(e: str): # e = error
    print(f"--------\n ACTIVATED FALLBACK PATH DETECTION, CAUSE:\n\n{e}")

    with open(os.path.join(scriptdir, r"\/SystemPrompt.LLMD"), "r") as n:
        systemprompt = n.read()

    with open(os.path.join(scriptdir, r"\/CharacterInfo.LLMD"), "r") as f:
        character = f.read()

    with open(os.path.join(scriptdir, r"\/CharacterName.LLMD"), "r") as n:
        charname = n.read()


try:

    with open(os.path.normpath(scriptdir + "/SystemPrompt.LLMD"), "r") as n:
        systemprompt = n.read()

    with open(os.path.normpath(scriptdir + "/CharacterInfo.LLMD"), "r") as f:
        character = f.read()

    with open(os.path.normpath(scriptdir + "/CharacterName.LLMD"), "r") as n:
        charname = n.read()

except Exception as e:
    fallback(e)

Username = "Bluro"

print('''
Remember, you can reset the LLM conversation using `!LLM reset`. You can also shut me down by doing `!LLM stop`.
\n
Do note this means that the person hosting the python file will need to restart it manually! be careful with what you're doing
''')

scriptdir2 = os.path.dirname(os.path.abspath(__file__))
target_file_path = os.path.normpath(scriptdir2 + "/Logs/CharacterLogs.LLMD")
filename = os.path.normpath(scriptdir2 + "/Logs/CharacterLogs")
print(f"Log File Directory: {target_file_path}")

def rename_log_file():
    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    new_filename = f"{filename}_{timestamp}.LLMD"

    # Check if the log file exists
    if os.path.exists(target_file_path,):
        # Rename the old log file
        try:
            os.rename(target_file_path, new_filename)
            print(f"Log file has been archived and reset succesfully. The file can be found at ` {new_filename} `")
        except Exception as e:
            print(f'''
That's weird...
Log file doesn't exist, or a weird error has occured within the code...
there may be something severely wrong with the code, or the dependencies in it!
\n
Please ping the owner of the bot!\n\n{e}
              ''')


log_renamed = False

def handle_stop(user_input):
    if user_input == "!LLM stop":
        print("The code for the bot is shutting down. Please ping the host if you wish to restart it.")
        exit()


while True:

    with open(target_file_path, 'r') as e:
        conversation = e.read()

    user_input = input(f"{Username}: ")
    handle_stop(user_input)
    if user_input == "!LLM reset":
        rename_log_file()
        log_renamed = True

    if log_renamed:
        log_renamed = False  # Reset the flag
        with open(target_file_path, 'w') as e:
            conversation = e.write("")
        continue  # Restart the loop

    Gentime_Start = datetime.now()

    # Prepare the data for the AI model
    data = dict(
        prompt=f'''
{systemprompt}
{character}
{conversation}
{Username}: {user_input}
{charname}:''',
        temperature=0.34,
        seed=10,
        top_p=1,
        top_k=0,
        top_a=0,
        length_penalty=1,
        encoder_rep_pen=1,
        frequency_penalty=0,
        presence_penalty=0,
        n=1,
        max_tokens=160,
        max_length=4096,
        guidance_scale=1,
        early_stopping=True,
        add_bos_token=True,
        truncation_length=2048,
        ban_eos_token=True,
        skip_special_tokens=True,
        stop=[f"{Username}:", "Guest:", f"{charname}:"]
    )

    # Call the AI model
    stream_response = requests.post(url, headers=headers, json=data, verify=False)
    client = sseclient.SSEClient(stream_response)

    # Process the response
    with open(target_file_path, 'a') as f:
        f.write(f"{Username}: {user_input}\n{charname}:")

    print(data['prompt'], end='')
    for event in client.events():
        payload = json.loads(event.data)
        print(payload['choices'][0]['text'], end='')
        with open(target_file_path, 'a') as f:
            f.write(payload['choices'][0]['text'])

    
    # End the timer and print the generation time
    Gentime_End = datetime.now()
    td = (Gentime_End - Gentime_Start).total_seconds()

    print(f'''
-----
LLM Generation time: {round(td, 3)}s''')

