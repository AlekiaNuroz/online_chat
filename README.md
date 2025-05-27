# Java Chat Server & Client

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

This application is a full-featured, multiroom chat system built in Java using Maven. It consists of a server and client, enabling real-time text communication between multiple users over a network. Users can join or create chat rooms (with optional password protection), send public or private messages, and use a variety of commands for enhanced interaction. The system supports emoji codes that are rendered as Unicode emojis, colored usernames, and timestamps for messages. Admin features include user moderation (kick, mute, ban), granting or revoking admin rights, and server shutdown. All actions are logged for auditing. The application is suitable for both small team collaboration and larger community chat environments.

---

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Commands](#commands)
- [Emoji Support](#emoji-support)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- Multiple chat rooms (with optional passwords)
- Private messaging
- Room and server admin commands
- Emoji support
- Colored usernames
- User listing and private messages
- Timestamps in client
- Logging for server and client actions

---

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven
- Running the server and connecting clients locally requires and environment variable to be set.
   ```powershell
   $Env:Environment="Local"
   ```

### powershell

1. **Run the server**
    ```powershell
    javac ChatServer.java
    java ChatServer.java
    ```
2. **Run the client**
    ```powershell
    javac ChatClient.java
    java ChatClient.java
    ```

### Usage
To use the chat server, start by launching the server with `java ChatServer`, which initializes the default "General" room and begins listening for connections on port 12345. Clients connect, receiving a default username (e.g., User1) and automatic placement in the General room. Once connected, users can chat freely in their current room, change their username (`/username Alice`), or send private messages (`/msg Bob Hello!`). Rooms are created or joined with `/join <roomName> [password]`. Password-protected rooms require the correct credentials for entry.

The server supports moderation tools for room admins (assigned via `/grant` or granted to room creators), including muting (`/mute Charlie 300` for a 5-minute mute), kicking (`/kick Charlie`), or banning (`/ban Charlie`) disruptive users. Admins can list muted users (`/muted`) or revoke privileges (`/revoke Mallory`). The server admin (username admin) has exclusive access to critical commands like `/shutdown`. For organization, users can list room members (`/users`), toggle message colors (`/color blue`), or return to the General room (`/exit`). All activity is logged to `chatserver.log`, including connections, commands, and moderation actions.

For optimal use, we recommend assigning descriptive room names, employing temporary mutes before bans, and changing default usernames promptly. The server handles disconnections gracefully, removing users from active rooms and notifying others of their departure.

<hr>

### Commands

Commands are entered in the chat input field, starting with a forward slash (`/`). Some commands take parameters. Parameters in agled brackets (&lt; & &gt;) are mandatory parameters in square brackets are optional. The server processes these commands and responds accordingly. Below are the available commands for both standard users and admins.

#### Standard Commands

<style>
.tg  {border-collapse:collapse;border-spacing:0;}
.tg td{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
  overflow:hidden;padding:10px 5px;word-break:normal;}
.tg th{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
  font-weight:normal;overflow:hidden;padding:10px 5px;word-break:normal;}
.tg .tg-llyw{border-color:inherit;color:#000000;background-color:#909090;text-align:left;vertical-align:top}
.tg .tg-0pky{border-color:inherit;text-align:left;vertical-align:top}
</style>
<table class="tg"><thead>
  <tr>
    <th class="tg-llyw">Command</th>
    <th class="tg-llyw">Description</th>
  </tr></thead>
<tbody>
  <tr>
    <td class="tg-0pky">/clear</td>
    <td class="tg-0pky">Clears the console (client only)</td>
  </tr>
  <tr>
    <td class="tg-0pky">/color &lt;colour&gt; </td>
    <td class="tg-0pky">Change your username color</td>
  </tr>
  <tr>
    <td class="tg-0pky">/exit</td>
    <td class="tg-0pky">Leave the current room and return to the General room</td>
  </tr>
  <tr>
    <td class="tg-0pky">/join &lt;room&gt; [password]</td>
    <td class="tg-0pky">Join or create a room</td>
  </tr>
  <tr>
    <td class="tg-0pky">/msg &lt;username&gt; &lt;message&gt;</td>
    <td class="tg-0pky">Send a private message</td>
  </tr>
  <tr>
    <td class="tg-0pky">/room</td>
    <td class="tg-0pky">Show your current room name</td>
  </tr>
  <tr>
    <td class="tg-0pky">/timestamp &lt;on\|off&gt;</td>
    <td class="tg-0pky">Enable or disable timestamps in client messages</td>
  </tr>
  <tr>
    <td class="tg-0pky">/username &lt;new name&gt;</td>
    <td class="tg-0pky">Change your username</td>
  </tr>
  <tr>
    <td class="tg-0pky">/users</td>
    <td class="tg-0pky">List users in the current room</td>
  </tr>
</tbody></table>

#### Admin Commands

<style>
.tg  {border-collapse:collapse;border-spacing:0;}
.tg td{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
  overflow:hidden;padding:10px 5px;word-break:normal;}
.tg th{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
  font-weight:normal;overflow:hidden;padding:10px 5px;word-break:normal;}
.tg .tg-llyw{border-color:inherit;color:#000000;background-color:#909090;text-align:left;vertical-align:top}
.tg .tg-0pky{border-color:inherit;text-align:left;vertical-align:top}
</style>
<table class="tg"><thead>
  <tr>
    <th class="tg-llyw">Command</th>
    <th class="tg-llyw">Description</th>
  </tr></thead>
<tbody>
  <tr>
    <td class="tg-0pky">/ban &lt;username&gt;</td>
    <td class="tg-0pky">Ban a user</td>
  </tr>
  <tr>
    <td class="tg-0pky">/grant &lt;username&gt;</td>
    <td class="tg-0pky">Grant admin rights</td>
  </tr>
  <tr>
    <td class="tg-0pky">/kick &lt;username&gt;</td>
    <td class="tg-0pky">Kick a user</td>
  </tr>
  <tr>
    <td class="tg-0pky">/mute &lt;username&gt;</td>
    <td class="tg-0pky">Mute a user</td>
  </tr>
  <tr>
    <td class="tg-0pky">/revoke &lt;username&gt;</td>
    <td class="tg-0pky">Revoke admin rights</td>
  </tr>
  <tr>
    <td class="tg-0pky">/shutdown</td>
    <td class="tg-0pky">Shutdown the server (server admin only)</td>
  </tr>
  <tr>
    <td class="tg-0pky">/unban &lt;username&gt;</td>
    <td class="tg-0pky">Unban a user</td>
  </tr>
  <tr>
    <td class="tg-0pky">/unmute &lt;username&gt;</td>
    <td class="tg-0pky">Unmute a user</td>
  </tr>
</tbody></table>

<hr>

### Emoji Support
The chat client supports emojis through a text-to-emoji mapping system, where users type shortcuts like `:smile:` or `:heart:` to send corresponding emojis (😊, ❤️). The client automatically converts these shortcuts into Unicode emojis before sending messages to the server and vice versa when displaying received messages. This approach ensures compatibility across all platforms, even if some terminals don’t natively render emojis, as the original shortcuts remain readable. The system includes 40+ common emojis (e.g., `:laugh:`, `:fire:`, `:pizza:`), covering emotions, objects, and symbols.

<style>
.tg  {border-collapse:collapse;border-spacing:0;}
.tg td{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
  overflow:hidden;padding:10px 5px;word-break:normal;}
.tg th{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
  font-weight:normal;overflow:hidden;padding:10px 5px;word-break:normal;}
.tg .tg-cpmz{border-color:inherit;background-color:#909090;color:#000000;text-align:left;vertical-align:top}
.tg .tg-baqh{border-color:inherit;text-align:center;vertical-align:top}
.tg .tg-0lax{border-color:inherit;text-align:center;vertical-align:top}
</style>
<table class="tg">
  <thead>
    <tr>
      <th class="tg-cpmz">Emoji Code</th>
      <th class="tg-cpmz">Unicode Emoji</th>
      <th class="tg-cpmz">Emoji Code</th>
      <th class="tg-cpmz">Unicode Emoji</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td class="tg-baqh">:100:</td>
      <td class="tg-0lax">💯</td>
      <td class="tg-baqh">:angry:</td>
      <td class="tg-0lax">😠</td>
    </tr>
    <tr>
      <td class="tg-baqh">:balloon:</td>
      <td class="tg-0lax">🎈</td>
      <td class="tg-baqh">:beer:</td>
      <td class="tg-0lax">🍺</td>
    </tr>
    <tr>
      <td class="tg-baqh">:boom:</td>
      <td class="tg-0lax">💥</td>
      <td class="tg-baqh">:brokenheart:</td>
      <td class="tg-0lax">💔</td>
    </tr>
    <tr>
      <td class="tg-baqh">:cake:</td>
      <td class="tg-0lax">🎂</td>
      <td class="tg-baqh">:check:</td>
      <td class="tg-0lax">✅</td>
    </tr>
    <tr>
      <td class="tg-baqh">:clap:</td>
      <td class="tg-0lax">👏</td>
      <td class="tg-baqh">:coffee:</td>
      <td class="tg-0lax">☕</td>
    </tr>
    <tr>
      <td class="tg-baqh">:confused:</td>
      <td class="tg-0lax">😕</td>
      <td class="tg-baqh">:cool:</td>
      <td class="tg-0lax">😎</td>
    </tr>
    <tr>
      <td class="tg-baqh">:cry:</td>
      <td class="tg-0lax">😭</td>
      <td class="tg-baqh">:eyes:</td>
      <td class="tg-0lax">👀</td>
    </tr>
    <tr>
      <td class="tg-baqh">:fire:</td>
      <td class="tg-0lax">🔥</td>
      <td class="tg-baqh">:gift:</td>
      <td class="tg-0lax">🎁</td>
    </tr>
    <tr>
      <td class="tg-baqh">:globe:</td>
      <td class="tg-0lax">🌍</td>
      <td class="tg-baqh">:heart:</td>
      <td class="tg-0lax">❤️</td>
    </tr>
    <tr>
      <td class="tg-baqh">:hug:</td>
      <td class="tg-0lax">🤗</td>
      <td class="tg-baqh">:kiss:</td>
      <td class="tg-0lax">😘</td>
    </tr>
    <tr>
      <td class="tg-baqh">:laugh:</td>
      <td class="tg-0lax">😂</td>
      <td class="tg-baqh">:lock:</td>
      <td class="tg-0lax">🔒</td>
    </tr>
    <tr>
      <td class="tg-baqh">:moon:</td>
      <td class="tg-0lax">🌙</td>
      <td class="tg-baqh">:music:</td>
      <td class="tg-0lax">🎵</td>
    </tr>
    <tr>
      <td class="tg-baqh">:ok:</td>
      <td class="tg-0lax">👌</td>
      <td class="tg-baqh">:phone:</td>
      <td class="tg-0lax">📱</td>
    </tr>
    <tr>
      <td class="tg-baqh">:pizza:</td>
      <td class="tg-0lax">🍕</td>
      <td class="tg-baqh">:poop:</td>
      <td class="tg-0lax">💩</td>
    </tr>
    <tr>
      <td class="tg-baqh">:pray:</td>
      <td class="tg-0lax">🙏</td>
      <td class="tg-baqh">:rainbow:</td>
      <td class="tg-0lax">🌈</td>
    </tr>
    <tr>
      <td class="tg-baqh">:robot:</td>
      <td class="tg-0lax">🤖</td>
      <td class="tg-baqh">:sad:</td>
      <td class="tg-0lax">😢</td>
    </tr>
    <tr>
      <td class="tg-baqh">:skull:</td>
      <td class="tg-0lax">💀</td>
      <td class="tg-baqh">:sleepy:</td>
      <td class="tg-0lax">😴</td>
    </tr>
    <tr>
      <td class="tg-baqh">:smile:</td>
      <td class="tg-0lax">😊</td>
      <td class="tg-baqh">:smirk:</td>
      <td class="tg-0lax">😏</td>
    </tr>
    <tr>
      <td class="tg-baqh">:snowflake:</td>
      <td class="tg-0lax">❄️</td>
      <td class="tg-baqh">:soccer:</td>
      <td class="tg-0lax">⚽</td>
    </tr>
    <tr>
      <td class="tg-baqh">:star:</td>
      <td class="tg-0lax">⭐</td>
      <td class="tg-baqh">:sun:</td>
      <td class="tg-0lax">☀️</td>
    </tr>
    <tr>
      <td class="tg-baqh">:surprised:</td>
      <td class="tg-0lax">😲</td>
      <td class="tg-baqh">:sweat:</td>
      <td class="tg-0lax">😅</td>
    </tr>
    <tr>
      <td class="tg-baqh">:tada:</td>
      <td class="tg-0lax">🎉</td>
      <td class="tg-baqh">:thinking:</td>
      <td class="tg-0lax">🤔</td>
    </tr>
    <tr>
      <td class="tg-baqh">:thumbsup:</td>
      <td class="tg-0lax">👍</td>
      <td class="tg-baqh">:unlock:</td>
      <td class="tg-0lax">🔓</td>
    </tr>
    <tr>
      <td class="tg-baqh">:vomit:</td>
      <td class="tg-0lax">🤮</td>
      <td class="tg-baqh">:warning:</td>
      <td class="tg-0lax">⚠️</td>
    </tr>
    <tr>
      <td class="tg-baqh">:wink:</td>
      <td class="tg-0lax">😉</td>
      <td class="tg-baqh">:x:</td>
      <td class="tg-0lax">❌</td>
    </tr>
  </tbody>
</table>


<hr>

### License
Copyright 2025 Brian Smith

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.