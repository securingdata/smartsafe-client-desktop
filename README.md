# SmartSafe Client

The SmartSafe project aims at providing a solution to securely store your passwords.
This password manager is split in several repositories: see the corresponding repository to have a complete description of each one.

The SmartSafe Client part consists in a rich application that allows a user to manage the Java Card smart card (for instance loading the Server application) and to use the password manager (create new entries, view entries, change the password, etc.).

## Security assumptions
The main security relies on the Java Card smart card that stores the sensitive data. The SmartSafe is assumed to be run on a trusted environment. Therefore attacks on the client implementation are not considered as relevant.

## Quick start

You need to have a Java Card Development Kit and Maven installed in order to build the project using the following command:

```
mvn clean compile assembly:single
```

The obtained JAR file can be executed by the following command:

```
java -jar SmartSafeClient-X.Y.Z-jar-with-dependencies.jar
```

## Road map
The following features are already developed:
 - Ability to connect to a card and perform user operations (password update, reading and writing groups and entries, etc.)

The following features are intended to be developed:
 - Secure messaging between the Client and the Server in order to avoid Man-in-the-middle and replay attacks
 - Ability to perform card content management from an admin password
 - Ability to retrieve information about memory usage in the smart card and other useful stats
 - Ability to set preferences (language, default settings, etc)
 - Adding help content in the rich interface