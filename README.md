# [@EPFLBot](https://t.me/EpflBot)
[![Build Status](https://travis-ci.org/epflbot/epflbot.svg?branch=master)](https://travis-ci.org/epflbot/epflbot)

Telegram Bot to access EPFL services (and more) from everyday chats.

Try it on Telegram [@EpflBot](https://t.me/EpflBot) (case insensitive).

To connect using your EPFL account and have some extra goodies, go to [@EPFLBot login](https://t.me/EpflBot?start=login)
or use the _/login_ command.

The bot runs in [privacy mode](https://core.telegram.org/bots#privacy-mode); only has access to _/commands_ and messages where it's **@mentioned**. It does **NOT** store passwords or sensitive credentials.

## Getting started

With docker, this will launch Elasticsearch for the storage and Kibana to access it (slow to start, about 2 minutes):

```shell
docker-compose up -d
```

On Linux, if docker fails to spawn Elasticsearch run ```sudo sysctl -w vm.max_map_count=262144```.

To enable Tequila authentication locally we recommend [ngrok](https://ngrok.com/).
```shell
ngrok http 8080
```

Then set your ngrok-provided URL (and port) in the [application.conf](https://github.com/epflbot/epflbot/blob/master/src/main/resources/application.conf).


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
