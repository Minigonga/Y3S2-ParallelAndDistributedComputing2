# CPD2

### Grade 18.17/20

### Group members:

1. Gonçalo Pinto (up202204943@up.pt)
2. Manuel Mo (up202205000@up.pt)

## How to compile and run the program

### 1. Compile
```shell
cd src # Go to the ./src directory
javac *.java storage/*.java # Compile all the .java files
```

### 2. Start the Server
```shell
# Terminal 1 (Server)
java ChatServer 8080 # Run server on port 8080

# Terminal 2 (Bot)
ollama run llama3 # Start the AI
```

### 3. Start clients
```shell
# Terminal 3 (Client 1)
java ChatClient 8080
# Terminal 4 (Client 2)
java ChatClient 8080
```
If you want more clients, open more terminals and do the same.

## Testing commands
```shell
AUTH Choose: 
1. Login 
2. Register 
3. Reconnect
4. Exit
> 4
AUTH Enter username: Alice
AUTH Enter password: 123
AUTH_SUCCESS Account created. Please login.
```
Do the same to login.

If you have closed your program. You can initialize it and reconnect without logging-in using your token. 

Also, if you were in some room before closing, you will automatically be redirected and joined to that room.

```shell
> 3
Enter your reconnection token: (token given)
AUTH_SUCCESS Welcom back (user).
Your new token is (new token)
```

After logging in, it shows all the rooms available. The user can enter one of them by writing down the name of the room. If the user wants to create a room, he just needs to write down aswell a different name as the ones created.

```shell
ROOMS Available rooms:
- general
- Calendar (AI Room) [Prompt: Calendar]

ROOMS Enter room name to join/create (or /exit to logout and disconnect):
> MyRoom  # Creates new room
...
```

If you want to create a room, you will have to select some options: Normal room or room with AI.
- 1. Normal room:
```shell
ROOM_TYPE Do you want to create an AI room? 
1. Yes 
2. No
> 2 #Select "No"

Room: MyRoom
To exit the room type /exit.
[Alice enters the room]
> Hello! # Write a message
```

- 2. AI room:
```shell
ROOM_TYPE Do you want to create an AI room? 
1. Yes 
2. No
> 1 # Select "Yes"
ROOM_PROMPT Enter the AI prompt/topic for this room:
> Weather # Write the topic of the room

Room: MyRoom (AI Room) [Prompt: Weather]
To exit the room type /exit.
[Alice enters the room]
> Hello! How is the weather today in Porto? # Ask him abbout something
Bot: Porto!

According to my current data, the weather in Porto, Portugal is... partly cloudy with a high chance of sunshine! It is a lovely day with temperatures around 18°C (64°F) and a gentle breeze.

Would you like me to check any specific forecast details or provide more information about Porto's weather conditions?
```

To get out of the room, you just need to write `/exit` and if you want to close the program, write again `/exit`.
