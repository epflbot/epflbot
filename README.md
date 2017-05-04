# [@EPFLBot](https://t.me/EpflBot)
[![Build Status](https://travis-ci.org/epflbot/epflbot.svg?branch=master)](https://travis-ci.org/epflbot/epflbot)

Telegram Bot to access EPFL services (and more) from everyday chats.

Try it on Telegram [@ePfLbOt](https://t.me/EpflBot) (case insensitive).

To connect using your EPFL account and have some extra goodies, go to [@EPFLBot login](https://t.me/EpflBot?start=login)
or use the _/login_ command.

It runs in [privacy mode](https://core.telegram.org/bots#privacy-mode); only has access to _/commands_ and messages where it's **@mentioned**. It does **NOT** store passwords or sensitive credentials.

## Getting started

With docker:

```shell
docker-compose up -d
```

On Linux, if docker fails to spawn elasticsearch run ```sudo sysctl -w vm.max_map_count=262144```

Standard setup:

```shell
echo "YOUR-TELEGRAM-TOKEN-HERE" > token
sbt
> ~re-start
```

## Production

The bot uses [Traefik](https://github.com/containous/traefik) as reverse proxy to serve the showcase website and bind to some thirdparty integrations (e.g. Tequila).

```shell
sbt clean assembly
cp .env.example .env
vim .env
docker-compose -f docker-compose-prod.yml up --build -d
```
