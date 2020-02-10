CREATE TABLE IF NOT EXISTS `event_buffs` (
  `player` varchar(30) NOT NULL,
  `buffs` varchar(300) NOT NULL,
  PRIMARY KEY (`player`)
);