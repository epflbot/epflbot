# EPFLbot
[![Build Status](https://travis-ci.com/mukel/epflbot.svg?token=d7f7szCri5ct4GZxRyqt&branch=master)](https://travis-ci.com/mukel/epflbot)

Telegram Bot for EPFL.

## Getting started

With docker:

```shell
docker run -d --name epflbot-es -p 9200:9200 -p 9300:9300 elasticsearch -E cluster.name="epflbot" -E transport.host=0.0.0.0
```

On Linux, if docker fails to spawn elasticsearch run ```sudo sysctl -w vm.max_map_count=262144```

Standard setup:

```shell
echo "YOUR-TELEGRAM-TOKEN-HERE" > token
sbt
> ~re-start
```

## Deploy

```shell
sbt clean assembly
cd dock
cp .env.example .env
vim .env
docker-compose up --build
```
