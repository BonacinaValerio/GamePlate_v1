var gameplate = cordova.require('cordova-plugin-firebase-gameplate.PluginFirebaseGameplate');

var bestScore;
var ranking;
var downloadRank;
var downloadRank2;
var downloadBest;
var arrayNull = [];

function initGame() {
	downloadRank = "false";
	downloadRank2 = "false";
	downloadBest = "false";
	ranking = {
		global: {
			crc32: null,
			rank: []
		},
		weekly: {
			crc32: null,
			rank: []
		}
	};
}

// salva il punteggio della partita e estrai la classifica aggiornata
function saveScore(score, bestScoreGlobal, bestScoreWeekly) {
	if (score > bestScoreGlobal || score > bestScoreWeekly) {
		gameplate.saveScore(score).then(function(result) {
			var global = result.global;
			ranking.global.rank = global.rank.complete(3);
			ranking.global.crc32 = global.crc32

			var weekly = result.weekly;
			ranking.weekly.rank = weekly.rank.complete(7);
			ranking.weekly.crc32 = weekly.crc32
			downloadRank = "true"
		}).catch(alert);
	}
	else {
		gameplate.getRank(score, ranking.global.crc32, ranking.weekly.crc32).then(function(result) {
			var global = result.global;
			if (global === null && ranking.global.rank.length == 0) 
				ranking.global.rank = arrayNull.complete(3);
			else if (global !== null) {
				ranking.global.rank = global.rank.complete(3);
				ranking.global.crc32 = global.crc32
			}
			var weekly = result.weekly;
			if (weekly === null && ranking.weekly.rank.length == 0) 
				ranking.weekly.rank = arrayNull.complete(7);
			else if (weekly !== null) {
				ranking.weekly.rank = weekly.rank.complete(7);
				ranking.weekly.crc32 = weekly.crc32
			}
			downloadRank = "true"
		}).catch(alert);
	}
}

// estrai i punteggi migliori [*global, *weekly]
function getBest() {
	gameplate.getBest().then(function(result) {
		bestScore = [result.global, result.weekly];
		downloadBest = "true"
	}).catch(alert);
}

// estrai la classifica aggiornata
function getRank() {
	gameplate.getRank(null, ranking.global.crc32, ranking.weekly.crc32).then(function(result) {
		var global = result.global;
		if (global === null && ranking.global.rank.length == 0) 
			ranking.global.rank = arrayNull.complete(3);
		else if (global !== null) {
			ranking.global.rank = global.rank.complete(3);
			ranking.global.crc32 = global.crc32
		}
		var weekly = result.weekly;
		if (weekly === null && ranking.weekly.rank.length == 0) 
			ranking.weekly.rank = arrayNull.complete(7);
		else if (weekly !== null) {
			ranking.weekly.rank = weekly.rank.complete(7);
			ranking.weekly.crc32 = weekly.crc32
		}
		downloadRank2 = "true"
	}).catch(alert);
}

// esci dal gioco
function exit() {
	gameplate.exit();
}

// completa l'array con campi di default oppure taglialo dagli eccessi
Array.prototype.complete= function(L){
	if (this.length<L) {
		for (var i = this.length; i < L; i++ ) {
			this[i] = ['-','-'];
		}
	}
	else if (this.length>L) {
		this.splice(L);
	}
	return this;
}
