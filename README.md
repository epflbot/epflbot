# EPFLbot

Telegram Bot for EPFL.

## Getting started

```shell
docker run -d --name epflbot-es -p 9200:9200 -p 9300:9300 elasticsearch -E cluster.name="epflbot"
echo "yourTelegramToken" > token
sbt
> ~re-start
```
