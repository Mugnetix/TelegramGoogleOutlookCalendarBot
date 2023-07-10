# TelegramEventBot
This is a chat bot for Telegram which allows you to look through and get notifications about your events from Google Calendar, Outlook Calendar or both.

The project requires 3 txt files (bot_config.txt, calendar_config.txt, notif_id.txt) and a .json credentials file from Google Console for the proper installation. All files must be located in the root directory of the project.

To get the .json file, create a project in Google Console with the Calendar API imported. Sample name: client_secret_.....json

bot_config.txt has to be filled out manually:
  Field "token" - insert your bot's token given by BotFather in Telegram;
  Field "credentials" - insert the name of the .json file containing your credentials from Google Console.

calendar_config.txt can be filled out manually or using the bot's interface in Telegram:
  Field "google" - insert your Google calendar identificator;
  Field "outlook" - insert the link to the .ics file of your Outlook Calendar.

notif_id.txt can be left without modification. Contains IDs of chats for which notifications are enabled.
