# --- !Ups

CREATE TABLE games (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    start DATETIME,
    end DATETIME,
    max_time INTEGER,
    top_left_latitude FLOAT,
    top_left_longitude FLOAT,
    top_right_latitude FLOAT,
    top_right_longitude FLOAT,
    bottom_left_latitude FLOAT,
    bottom_left_longitude FLOAT,
    bottom_right_latitude FLOAT,
    bottom_right_longitude FLOAT
);

CREATE TABLE teams (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	name VARCHAR,
	color VARCHAR,
	game_id INTEGER,
	
	FOREIGN KEY (game_id) REFERENCES games(id)
);

CREATE TABLE players (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	name VARCHAR,
	team_id INTEGER,
	money INTEGER,
	latitude FLOAT,
    longitude FLOAT,
	
	FOREIGN KEY (team_id) REFERENCES teams(id)
);

CREATE TABLE items (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	category VARCHAR,
	cost INTEGER,
	latitude FLOAT,
    longitude FLOAT,
    game_id INTEGER,
    
    FOREIGN KEY (game_id) REFERENCES games(id)
);

CREATE TABLE equipments (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	player_id INTEGER,
	item_id INTEGER,
	
	FOREIGN KEY (player_id) REFERENCES players(id),
	FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE treasures (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	closed INTEGER,
	key_id INTEGER,
	money INTEGER,
	item_id INTEGER,
	latitude FLOAT,
    longitude FLOAT,
    game_id INTEGER,
	
	FOREIGN KEY (key_id) REFERENCES items(id),
	FOREIGN KEY (item_id) REFERENCES items(id),
	FOREIGN KEY (game_id) REFERENCES games(id)
);

CREATE TABLE ghosts_levels (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	range_latitude INTEGER,
	range_longitude INTEGER,
	max_damage INTEGER
);

CREATE TABLE ghosts (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    game_id INTEGER,
    level_id INTEGER,
    status INTEGER,
    latitude FLOAT,
    longitude FLOAT,
    treasure_id INTEGER,
    
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (level_id) REFERENCES ghosts_levels(id),
    FOREIGN KEY (treasure_id) REFERENCES treasures(id)
);

# --- !Downs
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS treasures;
DROP TABLE IF EXISTS ghosts_levels;
DROP TABLE IF EXISTS ghosts;
DROP TABLE IF EXISTS games;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS equipments;