# Discord Bot Language Model Interface
This repo contains both a Terminal version and a Discord bot compatible with the [Text Generation WebUI](https://github.com/oobabooga/text-generation-webui) API Interface

## Branches and Credits

[Kotlin Variant](https://github.com/KarstenPH/Discord-Bot-LM-Interface/tree/kotlin): [@Superbox2147](https://github.com/Superbox2147)

Other Variants:
(Legacy, for archiving only) [**Terminal Only Variant**](https://github.com/KarstenPH/Discord-Bot-LM-Interface/tree/terminal): [@KarstenPH](https://github.com/KarstenPH)
(Incomplete and Private) Python Variant: [@fily.gif](https://github.com/fily-gif), [@KarstenPH](https://github.com/KarstenPH), & [@neurofumo (promote)](https://github.com/neurofumo)


# Features
Do note some of these are planned features!
- Grab either the Username or Nickname (Server or Default) from the chatter
- Support for conversation logs as text files with basic formatting
- Blacklisting with Bot Timeout support

Features that might not work:
- Multiple Conversations at once (One conversation at once, but no user amount limit)


# Documentation

Documentation (in simple and short terms) can be found below, But if you are a bit lost, confused, or want to change parameters, refer to the code itself

## Requirements:
1. [Oobabooga/Text Generation WebUI](https://github.com/oobabooga/text-generation-webui)
  (or any OpenAI API compatible alternative)
2. Run the text generation webui with the `--api --listen` flags
3. Open bot.py and modify the values inside of it to your choice, same with the Character data
4. Run the bot.py file
