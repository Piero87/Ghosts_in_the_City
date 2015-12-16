
// Global variables
var busterX = 0; // X position of buster
var busterY = 0; // Y position of buster
var canvas; // canvas
var ctx; // context
var emptyBack = new Image();

var gameFrame = 0;

var busters = [];
var busters_images = [];
var ghosts = [];
var ghosts_images = [];

var space_width = 500;
var space_heigth = 500;
var icon_dim = 32;
var move = 5;

// This function is called on page load.

function startGame() {

	// Get the canvas element.
	canvas = document.getElementById("gameArena");

	// Make sure you got it.
	if (canvas.getContext){
		// If you have it, create a canvas user interface element.
		
		// Specify 2d canvas type.
		ctx = canvas.getContext("2d");

		// Paint it black.
		ctx.fillStyle = "grey";
		ctx.rect(0, 0, space_width, space_heigth);
		ctx.fill();

		// Save the initial background.
		emptyBack = ctx.getImageData(0, 0, icon_dim, icon_dim); 
		
	}

	// Play the game until the until the game is over.
	gameLoop = setInterval(doGameLoop, 16);

	// Add keyboard listener.
	window.addEventListener('keydown', whatKey, true);

}

function addBuster(id, name, x, y) {

	var buster = {};
	buster.id = id;
	buster.name = name;
	buster.x = x; // current buster position X
	buster.y = y; // current buster position Y
	buster.old_x = x; // old buster position X
	buster.old_y = y; // old buster position Y
	busters.push(buster);

	var buster_img = new Image(); // buster
	buster_img.src = 'G' + (busters.length) + '_right.png';
	busters_images.push(buster_img);

}

function busterMove(id, x, y) {
	for (var i = 0 ; i < busters.length; i++){
		if (busters[i].id == id) {
			if (busters[i].x - x < 0) {
				busters_images[i].src = 'G' + (i+1) + '_left.png';
			} else {
				busters_images[i].src = 'G' + (i+1) + '_right.png';
			}
			busters[i].old_x = busters[i].x;
			busters[i].old_y = busters[i].y;
			busters[i].x = x;
			busters[i].y = y;
		}
	}
}

function addGhost(id, x, y) {

	var ghost = {};
	ghost.id = id;
	ghost.x = x; // current ghost position X
	ghost.y = y; // current ghost position Y
	ghost.old_x = x; // old ghost position X
	ghost.old_y = y; // old ghost position Y
	ghosts.push(ghost);

	var ghost_img = new Image(); // ghost
	ghost_img.src = 'Ghost_right.png';
	ghosts_images.push(ghost_img);

}

function ghostMove(id, x, y) {
	for (var i = 0 ; i < ghosts.length; i++){
		if (ghosts[i].id == id) {
			if (ghosts[i].x - x < 0) {
				ghosts_images[i].src = 'Ghost_left.png';
			} else {
				ghosts_images[i].src = 'Ghost_right.png';
			}
			ghosts[i].old_x = ghosts[i].x;
			ghosts[i].old_y = ghosts[i].y;
			ghosts[i].x = x;
			ghosts[i].y = y;
		}
	}
}

function doGameLoop() {
	/*
	// redraw busters positions
	for (var i = 0; i < busters.length; i++) {
		ctx.putImageData(emptyBack, busters[i].old_x, busters[i].old_y);
		ctx.drawImage(busters_images[i], busters[i].x, busters[i].y);
	}
	
	// redraw ghosts positions
	for (var i = 0; i < ghosts.length; i++) {
		ctx.putImageData(emptyBack, ghosts[i].old_x, ghosts[i].old_y);
		ctx.drawImage(ghosts_images[i], ghosts[i].x, ghosts[i].y);
	}*/

}

// Get key press.
function whatKey(evt) {

	var i = 0;
	
	// Arrows keys
	var arrows = [37, 38, 39, 40];
	
		// Flag to put variables back if we hit an edge of the board.
		var flag = 0;
	
		// Get where the buster was before key process.
		busters[i].old_x = busters[i].x;
		busters[i].old_y = busters[i].y;
	
	if (arrows.indexOf(evt.keyCode) == -1) return false;
	
	switch (evt.keyCode) {

		// Left arrow.
	case 37:
		busters_images[i].src = 'G' + busters[i].index + '_left.png';
		busters[i].x = busters[i].x - move;
		if (busters[i].x < 0) {
			// If at edge, reset buster position and set flag.
			busters[i].x = 0;
			flag = 1;
		}
		break;

		// Right arrow.
	case 39:
		busters_images[i].src = 'G' + busters[i].index + '_right.png';
		busters[i].x = busters[i].x + move;
		if (busters[i].x > (space_width - icon_dim)) {
			// If at edge, reset buster position and set flag.
			busters[i].x = space_width - move;
			flag = 1;
		}
		break;

		// Down arrow
	case 40:
		busters[i].y = busters[i].y + move;
		if (busters[i].y > (space_heigth - icon_dim)) {
			// If at edge, reset buster position and set flag.
			busters[i].y = space_heigth - move;
			flag = 1;
		}
		break;

		// Up arrow 
	case 38:
		busters[i].y = busters[i].y - move;
		if (busters[i].y < 0) {
			// If at edge, reset buster position and set flag.
			busters[i].y = 0;
			flag = 1;
		}
		break;

	}

	// If flag is set, the buster did not move.
	// Put everything backBuster the way it was.
	if (flag) {
		busters[i].x = busters[i].old_x;
		busters[i].y = busters[i].old_y;
	}
}
	