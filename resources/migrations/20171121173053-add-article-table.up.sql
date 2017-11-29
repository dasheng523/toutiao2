CREATE TABLE articles (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parse_time` int(11) DEFAULT NULL,
  `source_url` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `article` longtext,
  PRIMARY KEY (`id`),
  UNIQUE KEY `source_url_index` (`source_url`)
) DEFAULT CHARSET=utf8