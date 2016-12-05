# EPFLbot

Telegram Bot for EPFL.

## Getting started

With docker:

```shell
docker run -d --name epflbot-es -p 9200:9200 -p 9300:9300 elasticsearch -E cluster.name="epflbot"
curl http://localhost:9200/survey -X PUT
curl http://localhost:9200/feedback -X PUT
```

Standard setup:

```shell
echo "YOUR-TELEGRAM-TOKEN-HERE" > token
sbt
> ~re-start
```
