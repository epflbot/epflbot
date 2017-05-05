All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](http://semver.org/).


## [0.2.0] - 2017-05-04

### Added
- Bot microsite.
- Satellite interactive beer menu /beers.
- Proper config managment (using Ficus).
- Bus interactive schedule for 701 and 705 /bus.
- Tequila Authentication /login /logout and /status.
- Ability to search and contact authenticated members via Telegram.
- Make Scalafmt style check mandatory.
- Add filtering to /menus.

### Changed
- Upgrade to Scala 2.12.2.
- Refactor/simplify docker env. with docker-compose.
- Major upgrade of ElasticSearch.
- Move to new internal domain.

### Fixed
- Add scrap-less directory search.
- Scrap EPFL users from LDAP.
- Fix /metro scrapping; make it robust.
- Fix truncation in survey choices.
- Warn about empty feedback.
- Improve /help command.
- Make inline search instantaneous.
- Cosmetic improvements to several commands.
