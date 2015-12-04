# --- !Ups

# --- Obiettivo dei giocatori, suddivisi in due squadre avversarie, 
# --- è quello di muoversi per la città, scoprire gli scrigni ed accumulare
# --- il maggior quantitativo di tesoro possibile. 
CREATE TABLE players (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	name VARCHAR,
	team_id INTEGER,
	money INTEGER,
	latitude FLOAT,
    longitude FLOAT
	
	FOREIGN KEY (team_id) REFERENCES teams(id)
);

# --- Ogni giocatore possiede uno "zaino" in cui conservare gli oggetti trovati durante il
# --- gioco.
CREATE TABLE equipments (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	player_id INTEGER,
	item_id INTEGER
	
	FOREIGN KEY (player_id) REFERENCES players(id)
	FOREIGN KEY (item_id) REFERENCES items(id)
);

# --- Esistono diversi tipi di oggetti nel gioco: chiavi per aprire gli scrigni, trappole
# --- per bloccare (temporaneamente) i fantasmi. Questi oggetti possono essere contenuti
# --- in uno scrigno oppure sparsi per l'area di gioco.
CREATE TABLE items (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	type VARCHAR,
	cost INTEGER,
	latitude FLOAT,
    longitude FLOAT,
    game_id INTEGER
    
    FOREIGN KEY (game_id) REFERENCES games(id)
);

CREATE TABLE teams (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	name VARCHAR,
	color VARCHAR,
	game_id INTEGER
	
	FOREIGN KEY (game_id) REFERENCES games(id)
);

# --- Tra gli scrigni, alcuni di questi sono chiusi e richiedono una chiave
# --- per la loro apertura contenuta a loro volta in un altro scrigno dislocato 
# --- in qualche altra posizione della città.
CREATE TABLE treasures (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	closed INTEGER,
	key_id INTEGER,
	money INTEGER,
	object_id INTEGER,
	latitude FLOAT,
    longitude FLOAT,
    game_id INTEGER
	
	FOREIGN KEY (key_id) REFERENCES objects(id)
	FOREIGN KEY (object_id) REFERENCES objects(id)
	FOREIGN KEY (game_id) REFERENCES games(id)
);

# --- A guardia di alcuni scrigni e, in generale, in movimento per la città i giocatori 
# --- possono incontrare dei fantasmi. I fantasmi sono in grado di percepire i  giocatori 
# --- ad una certa distanza da loro ed inseguirli per rubare loro il quantitativo 
# --- monetario eventualmente guadagnato fino a quel momento. I fantasmi sono tuttavia 
# --- sensibili a trappole sul terreno in cui possono cadere se queste sono posizionate
# --- dai giocatori sul percorso seguito dai fantasmi.
# --- Ogni fantasma è collegato ad un tesoro a cui trasferisce magicamente i soldi nel
# --- momento in cui li ruba al giocatore.
CREATE TABLE ghosts (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    game_id INTEGER,
    level_id INTEGER,
    status INTEGER,
    latitude FLOAT,
    longitude FLOAT,
    treasure_id INTEGER
    
    FOREIGN KEY (game_id) REFERENCES games(id)
    FOREIGN KEY (level_id) REFERENCES ghosts_levels(id)
    FOREIGN KEY (treasure_id) REFERENCES treasures(id)
);

# --- Ogni fantasma ha un livello di potenza che definisce quanto è ampio il suo raggio di
# --- percezione e qual è il massimo quantitativo di denaro che può rubare al giocatore. 
CREATE TABLE ghosts_levels (
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
	range_latitude INTEGER,
	range_longitude INTEGER,
	max_damage INTEGER
);

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

# --- !Downs
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS treasures;
DROP TABLE IF EXISTS ghosts_levels;
DROP TABLE IF EXISTS ghosts;
DROP TABLE IF EXISTS games;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS equipments;