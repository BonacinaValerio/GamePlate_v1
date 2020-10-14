// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const admin = require('firebase-admin');

const NodeRSA = require('node-rsa');

const { GeoCollectionReference, GeoFirestore, GeoQuery, GeoQuerySnapshot } = require('geofirestore');

admin.initializeApp();

exports.onRansomTicket = functions.https.onCall((data, context) => {
	var user;
	const lang = data.lang.toUpperCase()
	const ticketCode = data.ticketCode

	// controlla attributi.
	if (!(typeof ticketCode === 'string') || lang.length === 0 ||
		!(typeof lang === 'string') || lang.length === 0)
		// Throwing an HttpsError.
		throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
			'ticketCode');

	// controlla se l'utente è autenticato.
	if (!context.auth) {
	  // Throwing an HttpsError.
	  throw new functions.https.HttpsError('failed-precondition', 'The function must be called ' +
	      'while authenticated with your user.');
	}
	else 
		user = context.auth.uid;

	const queryAdminEnable = admin.database().ref(`/users/${user}/privateKey`).once('value');

	return queryAdminEnable.then((snapshot) => {
		if (!snapshot.exists())
			throw notAllowedException();
		else {
			const privateKey = snapshot.val();
			const decrypted = decrtyptCode(privateKey, ticketCode);

			const ticket = JSON.parse(decrypted);
			return ransomTicket(ticket, lang);
		}
	}).catch(err => {
		if (err.name === 'notAllowedException') {
			return console.log('ransomTicket not allowed, user: ' + user)
		}
		else {
			var stack = err.stack.toString().split(/\r\n|\n/);
			return console.log('error!'+err, '   [' + stack[1] + ']')
		}
	});
});

function ransomTicket(ticket, lang) {
	const idReward = ticket.idReward
	const ticketReward = ticket.ticketReward
	const user = ticket.user
	var response = {}

	var collection = admin.firestore().collection('reward');
	var doc = collection.doc(idReward)

	var data;
	return doc.get().then((docSnap) => {
		data = docSnap.data()

		const restaurantId = data.restaurantId
		return admin.firestore().collection('restaurants').doc(restaurantId).get()
	}).then((docSnap) => {
		const dataRest = docSnap.data();
		var allProm = [];

		const allLang = dataRest.lang
		if (!allLang.includes(lang)) {
			lang = 'EN'
		}

		const ticket = data.users[user][ticketReward]

		const deadline = ticket.deadline
		const ransomDate = ticket.ransomDate

		const todayAsTimestamp = admin.firestore.Timestamp.now()

		if (deadline < todayAsTimestamp) {
			if (ransomDate) {
				response['status'] = 'TICKET_USED'
				response['details'] = ransomDate.toMillis();
			}
			else {
				response['status'] = 'TICKET_EXPIRED'
				response['details'] = deadline.toMillis()
			}
		}
		else {
			if (ransomDate) {
				response['status'] = 'TICKET_USED'
				response['details'] = ransomDate.toMillis();
			}
			else {
				response['status'] = 'TICKET_VALID'
				const typeString = 'reward_'+data.type
				var queryTerms = admin.database().ref(`/strings/${lang}/${typeString}/terms`).once('value');
				allProm.push(disableTicketReward(collection, idReward, user, ticketReward, true))
				allProm.push(Promise.all([data.type, queryTerms]))
			}
		}
		return Promise.all(allProm);
	}).then((allProm) => {
		if (allProm.length > 0) {
			const queryTerms = allProm[1]
			var detail = {}
			detail['title'] = queryTerms[0]
			detail['terms'] = queryTerms[1].val()
			response['details'] = detail
		}

		return response;
	})
}

let notAllowedException = () => {
	let err = new Error();
	err.name = 'notAllowedException';
	return err;
};

exports.onTicketEarned = functions.database.ref('/users/{user}/reward/{ticketReward}').onCreate((snap, context)  => {
    const user = context.params.user;
    const data = snap.val();
    const restaurantId = data.restaurantId;

	var collection = admin.firestore().collection('restaurants').doc(restaurantId);
	var queryRestaurant = collection.get();

	const queryNotificationEnable = admin.database().ref(`/users/${user}/enableNotification`).once('value');

	// Notification details.
	const payload = {
		notification: {}
	};

	let restaurant;
	let lang;
	// The snapshot to the user's tokens.
	let tokensSnapshot;

	// The array containing all the user's tokens.
	let tokens;

	return queryNotificationEnable.then((enableSnapshot) => {
		if (enableSnapshot.exists()) {
			const enable = enableSnapshot.val();
			if (enable) 
				return queryRestaurant;
			else
				throw notificationDisable();
		}
		else 
			return queryRestaurant;
	}).then((docSnap) => {
		restaurant = docSnap.data().restaurant;
		lang = docSnap.data().lang;

		return admin.database().ref(`/users/${user}/lang`).once('value');
	}).then((langSnapshot) => {

		var userLang = langSnapshot.val();
		if (!lang.includes(userLang)) 
			userLang = 'EN';

		return admin.database().ref(`/strings/${userLang}/notification`).once('value');

	}).then((messageSnap) => {
		const message = messageSnap.val();
		payload.notification.body = message.body + restaurant;
		payload.notification.title = message.title;
		// Get the list of device notification tokens.
		return admin.database().ref(`/users/${user}/notificationTokens`).once('value');
	}).then((tokensSnapshot0) => {
		tokensSnapshot = tokensSnapshot0;
		tokens = Object.keys(tokensSnapshot.val());
		return admin.messaging().sendToDevice(tokens, payload)
	}).then((response) => {
		const tokensToRemove = [];
		response.results.forEach((result, index) => {
			const error = result.error;
			if (error) {
				console.error('Failure sending notification to', tokens[index], error);
				// Cleanup the tokens who are not registered anymore.
				if (error.code === 'messaging/invalid-registration-token' ||
				error.code === 'messaging/registration-token-not-registered') {
					tokensToRemove.push(tokensSnapshot.ref.child(tokens[index]).remove());
				}
			}
		});
		return Promise.all(tokensToRemove);
	}).catch(err => {
		if (err.name === 'notificationDisable') {
			return console.log('notification trigger disabled, user: ' + user)
		}
		else {
			var stack = err.stack.toString().split(/\r\n|\n/);
			return console.log('error!'+err, '   [' + stack[1] + ']')
		}
	});
});

let notificationDisable = () => {
	let err = new Error();
	err.name = 'notificationDisable';
	return err;
};

exports.checkEnableReward = functions.https.onRequest ((req, res) => {
	var collection = admin.firestore().collection('reward');

	const todayAsTimestamp = admin.firestore.Timestamp.now()
	var queryReward = collection
		.where("enable", "==", true)
		.where("endAt", "<=", todayAsTimestamp)
		.get();

	return queryReward.then((querySnapshot) => {
		allProm = [];
		querySnapshot.forEach((docSnap) => {
			var data = docSnap.data();
			var newState = {enable : false}
	
			if (data.type === 'Weekly') {
				var endAtMillis = data.endAt.toMillis();

				var newEndAtTimestamp = admin.firestore.Timestamp.fromMillis(endAtMillis+(86400000*7))
				newState['endAt'] = newEndAtTimestamp;


				var newStartAtTimestamp = admin.firestore.Timestamp.fromMillis(endAtMillis+1000)
				newState['startAt'] = newStartAtTimestamp;


				allProm.push(earnRewardAndClosing(data, docSnap.id, collection));

				delete newState.enable;
			}
			else {
				// aggiorna punteggio reward per la ordinaper 
				var refDocRewardScore = admin.firestore().collection('coordinates').doc(data.restaurantId)
				allProm.push(decreaseRewardScore(refDocRewardScore))
			}

			var ref = docSnap.ref
			var query = ref.update(newState);



			allProm.push(query)
		});
		return Promise.all(allProm);

	}).then(() => {
		return res.status(200).send("ok");
	}).catch(e => {
		var stack = e.stack.toString().split(/\r\n|\n/);
		return res.status(500).send('error!'+e, '   [' + stack[1] + ']');
	});
});

function decreaseRewardScore(refDocRewardScore) {
	return refDocRewardScore.get().then((docSnap) => {
		var rewardScore = docSnap.data().d.reward;

		var rewardScoreUpdate = {};
		rewardScoreUpdate[`d.reward`] = --rewardScore
		return docSnap.ref.update(rewardScoreUpdate);
	})
}

function increaseRelevanceScore(refDocRelevanceScore) {
	return refDocRelevanceScore.get().then((docSnap) => {
		var relevanceScore = docSnap.data().d.relevance;

		var relevanceScoreUpdate = {};
		relevanceScoreUpdate[`d.relevance`] = ++relevanceScore
		return docSnap.ref.update(relevanceScoreUpdate);
	})
}

exports.checkEnableTicket = functions.https.onRequest ((req, res) => {
	// controlla la scadenza dei ticket erogati per ogni reward

	var collection = admin.firestore().collection('reward');

	// ordino per escludere i reward che non hanno nessun vincitore
	var queryReward = collection
		.orderBy('users')
		.get();

	return queryReward.then((querySnapshot) => {
		const todayAsMillis = admin.firestore.Timestamp.now();
		allProm = [];
		querySnapshot.forEach((docSnap) => {
			var data = docSnap.data();

			var listUsers = data.users;
			for (const user in listUsers) {
				const listTicketRewards = listUsers[user]
				for(const ticketReward in listTicketRewards) {
					if (listTicketRewards[ticketReward].enable === true && 
						listTicketRewards[ticketReward].deadline <= todayAsMillis) {
						allProm.push(disableTicketReward(collection, docSnap.id, user, ticketReward));
					}
				}
			}

		});
		return Promise.all(allProm);

	}).then(() => {
		return res.status(200).send("ok");
	}).catch(e => {
		var stack = e.stack.toString().split(/\r\n|\n/);
		return res.status(500).send('error!'+e, '   [' + stack[1] + ']');
	});
});

function disableTicketReward(collection, rewardId, user, ticketReward, setRansomeDate = false) {
	var refTicketReward = admin.database().ref('users/'+user+'/reward/'+ticketReward+'/enable');
	var allProm = [];
	allProm.push(refTicketReward.set(false))

	var usersUpdate = {};
	usersUpdate[`users.${user}.${ticketReward}.enable`] = false
	if (setRansomeDate) {
		usersUpdate[`users.${user}.${ticketReward}.ransomDate`] = admin.firestore.Timestamp.now();
	}
	allProm.push(collection.doc(rewardId)
		.update(usersUpdate));

	return Promise.all(allProm);
}

function earnRewardAndClosing(dataReward, rewardId, collection) {
	// chiudi la classifica settimanale e premia i vincitori
	var ref = admin.database().ref('game/'+dataReward.url+'/week')
	var numberWeek;
	var winner;
	return ref.once('value').then((snapshot) => {
		numberWeek = snapshot.val();
		ref.set(numberWeek+1);
		var refRanking = admin.database().ref('rankings/'+dataReward.url+'/weekly'+numberWeek);
		return refRanking.orderByKey().limitToLast(1).once("value")
	}).then((snapshot) => {

		var refFirstPos;
		snapshot.forEach((snap) => {
			refFirstPos = snap.ref
		})

		if (refFirstPos === undefined) {
			throw noWinnerFound();
		}
		
		return refFirstPos.orderByChild('index').limitToFirst(1).once("value")
	}).then((snapshot) => {
		
		snapshot.forEach((snap) => {
			winner = snap.key
		})

		return admin.firestore().collection('restaurants').doc(dataReward.restaurantId).get()
	}).then((docSnap) => {
		const rest = docSnap.data()

		const restaurant = rest.restaurant;
		const game = rest.game;
		const background = rest.background;

		var type = dataReward.type;
		if (dataReward.extra !== null) {
			type += dataReward.extra;
		}

		var allProm = [];
		const validityTime = dataReward.validityTime;
		const today = admin.firestore.Timestamp.now();
		const todayAsMillis = today.toMillis();
		const deadlineTimestamp = admin.firestore.Timestamp.fromMillis(todayAsMillis+validityTime);
		var refUser = admin.database().ref('users/'+winner+'/reward');
		var newRefTicketReward = refUser.push();
		var newTicketReward = {
			gameId: dataReward.url,
			restaurantId: dataReward.restaurantId,
			idReward: rewardId,
			ticketCode: generateCode(dataReward.publicKey, rewardId, winner, newRefTicketReward.key),
			deadline: todayAsMillis+validityTime,
			enable: true,
			startAt: todayAsMillis,
			type: type,
			restaurant: restaurant,
			game: game,
			background: background
		};

		allProm.push(newRefTicketReward.update(newTicketReward));

		var usersUpdate = {};
		usersUpdate[`users.${winner}.${newRefTicketReward.key}`] = {
			deadline: deadlineTimestamp,
			enable: true
		}
		allProm.push(collection.doc(rewardId)
			.update(usersUpdate));

		return Promise.all(allProm);
	}).catch(err => {
		if (err.name === 'noWinnerFound') {
			return console.log(dataReward.url +' weekly'+ numberWeek+': '+'no winner found')
		}
		else {
			var stack = err.stack.toString().split(/\r\n|\n/);
			return console.log('error!'+err, '   [' + stack[1] + ']')
		}
	});
}

let noWinnerFound = () => {
	let err = new Error();
	err.name = 'noWinnerFound';
	return err;
};
 
function checkRewardEarned(user, game, score) {
	// controlla se è stato vinto un premio

	if (typeof score !== 'number') 
		return;

	var collection = admin.firestore().collection('reward');
	var queryReward = collection
		.where("enable", "==", true)
		.where("url", "==", game)
		.get();

	var ticketsEarned = [];
	queryReward.then((querySnapshot) => {
		var savedRest = {};

		var allProm = [];

		var indexProm = 0;
		querySnapshot.forEach((docSnap) => {
			var data = docSnap.data()
			data['docId'] = docSnap.id;

			var type = data.type

			// weekly non viene gestito qui
			if (type !== 'Weekly') {
				// controllo per evitare che un utente vinca più volte lo stesso reward
				// perchè a differenza dei weekly i reward di altro tipo una volta scaduti
				// non posso essere riattivati
				if (data.users === undefined || data.users[user] === undefined) {

					var goal = data.goal;

					if (score >= goal) {
						
						if (savedRest[data.restaurantId] === undefined) {
							savedRest[data.restaurantId] = indexProm;
							allProm.push(Promise.all(
								[data, admin.firestore().collection('restaurants').doc(data.restaurantId).get()]))
						}
						else {
							allProm.push(Promise.all(
								[data, savedRest[data.restaurantId]]));	
						}
						indexProm ++;
					}
				}
			}
		});
		return Promise.all(allProm);
	}).then((restSnapshot) => {
		var allProm = [];
		restSnapshot.forEach((result) => { 
			const dataReward = result[0]
			const dataRest = result[1]

			var rest;
			if (typeof dataRest !== 'number') 
				rest = dataRest.data();
			else 
				rest = restSnapshot[dataRest][1].data();

			const restaurant = rest.restaurant;
			const game = rest.game;
			const background = rest.background;

			var type = dataReward.type

			if (dataReward.extra !== null) {
				type += dataReward.extra;
			}

			const validityTime = dataReward.validityTime;
			const today = admin.firestore.Timestamp.now();
			const todayAsMillis = today.toMillis();
			const deadlineTimestamp = admin.firestore.Timestamp.fromMillis(todayAsMillis+validityTime);

			var refUser = admin.database().ref('users/'+user+'/reward');
			var newRefTicketReward = refUser.push();
			var newTicketReward = {
				gameId: dataReward.url,
				restaurantId: dataReward.restaurantId,
				idReward: dataReward.docId,
				ticketCode: generateCode(dataReward.publicKey, dataReward.docId, user, newRefTicketReward.key),
				deadline: todayAsMillis+validityTime,
				enable: true,
				startAt: todayAsMillis,
				type: type,
				restaurant: restaurant,
				game: game,
				background: background
			};

			allProm.push(newRefTicketReward.update(newTicketReward));

			var usersUpdate = {};
			usersUpdate[`users.${user}.${newRefTicketReward.key}`] = {
				deadline: deadlineTimestamp,
				enable: true
			}
			allProm.push(collection.doc(dataReward.docId)
				.update(usersUpdate));
		});
		return Promise.all(allProm);
	}).catch((e) => {
		var stack = e.stack.toString().split(/\r\n|\n/);
		return console.log('error on earning ticketReward: '+e, '   [' + stack[1] + ']')
	});


	var queryRestaurantId = admin.database().ref('game/'+game+'/restaurantId');
	queryRestaurantId.once('value').then((snapshot) => {
		const id = snapshot.val();
		const refDocRelevanceScore = admin.firestore().collection('coordinates').doc(id)
		return increaseRelevanceScore(refDocRelevanceScore)
	}).catch((e) => {
		var stack = e.stack.toString().split(/\r\n|\n/);
		return console.log('error on increaseRelevanceScore: '+e, '   [' + stack[1] + ']')
	});

}

function generateCode(publicKey, idReward, user, ticketReward) {
	const key = new NodeRSA(publicKey);
	const content = { 
		idReward: idReward,
		user: user,
		ticketReward: ticketReward
	}
	const encrypted = key.encrypt(JSON.stringify(content), 'base64');
	return encrypted;
}

function decrtyptCode(privateKey, content) {
    const key = new NodeRSA(privateKey);
	const decrypted = key.decrypt(content, 'utf8');
	return decrypted;
}

// ritorna i ristoranti che si trovano entro i bounds definiti
exports.onSearchHere = functions.https.onCall((data, context) => {

	const lang = data.lang.toUpperCase();
	const minLat = data.minLat;
	const minLng = data.minLng;
	const maxLat = data.maxLat;
	const maxLng = data.maxLng;
	const centerLat = data.centerLat;
	const centerLng = data.centerLng;

	const distance = data.distance; 

	// controlla attributi.
	if (!(typeof lang === 'string') || lang.length === 0 ||
		!(typeof minLat === 'number') || !(typeof maxLat === 'number') ||
		!(typeof minLng === 'number') || !(typeof maxLng === 'number') || 
		!(typeof centerLat === 'number') || !(typeof centerLng === 'number') || 
		!(typeof distance === 'number'))
		// Throwing an HttpsError.
		throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
			'lang, minLat, minLng, maxLat, maxLng, centerLat, centerLng, distance');

	return search(lang, centerLat, centerLng, distance, -1, 25, minLat, minLng, maxLat, maxLng);

});

// ritorna i ristoranti vicino al punto dato
exports.onFindMe = functions.https.onCall((data, context) => {

	const lang = data.lang.toUpperCase();
	const lat = data.lat;
	const lng = data.lng;
	const distance = data.distance;
	const orderBy = data.orderBy;

	// controlla attributi.
	if (!(typeof lang === 'string') || lang.length === 0 ||
		!(typeof lat === 'number') ||
		!(typeof lng === 'number') || 
		!(typeof distance === 'number') || distance < 10 || distance > 50 ||
		!(typeof orderBy === 'number') || (orderBy !== 0 && orderBy !== 1 && orderBy !== 2)) 
		// Throwing an HttpsError.
		throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
			'lang, lat, lng, distance, orderBy');

	return search(lang, lat, lng, distance, orderBy, -1);
});

// ritorna un solo ristorante
exports.onGetOneRestaurant = functions.https.onCall((data, context) => {

	const lang = data.lang.toUpperCase();
	const id = data.id;
	if (!(typeof id === 'string') || id.length === 0 || 
		!(typeof lang === 'string') || lang.length === 0)
		// Throwing an HttpsError.
		throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
			'id, lang');

	var firestore = admin.firestore();
	let ref = firestore.collection('coordinates');

	var lat;
	var lng;
	var response;

	return ref.doc(id).get().then((snapshot) => {
		if (!snapshot.exists) 
			return null;

		var data = snapshot.data().d;
		lat = data.latitude;
		lng = data.longitude;

		return data.ref.get();
	}).then((snap) => {
		response = snap.data();
		response['lat'] = lat 
		response['lng'] = lng

		var newLang = lang;
		if (!response['lang'].includes(lang))
			newLang = 'EN'

		response['lang'] = newLang;

		return admin.database().ref('strings/'+newLang+'/'+response['url']+'_desc').once('value');
	}).then((desc) => {
		response['description'] = desc.val();
		return handleOneLang(response);
	}).then((restaurant) => {
		return {result : restaurant};
	});
});

function handleOneLang(restaurant) {
	var collection = admin.firestore().collection('reward');
	var allQueryReward = [];

	var queryReward = collection.where("url", "==", restaurant['url'])
		.where("restaurantId", "==", restaurant['id'])
		.where("enable", "==", true).get();

	return queryReward.then((querySnapshot) => {

		restaurant['rewards'] = [];
		var savedProm = {}

		var allStringProm = [];
		var indexProm = 0;
		querySnapshot.forEach((docSnap) => {
			var data = docSnap.data()
			delete data.enable;
			delete data.goal;
			delete data.publicKey;
			delete data.validityTime;
			delete data.users;

			restaurant['rewards'].push(data);

			var type = data.type
			if (data.extra !== null) 
				type += data.extra
			if (savedProm[type] === undefined) {
				savedProm[type] = indexProm;
				var ref = admin.database().ref('strings/'+restaurant['lang']+'/reward_'+type);
				allStringProm.push(ref.once('value'))
			}
			else {
				allStringProm.push(savedProm[type])
			}

			indexProm++;
		});
		return Promise.all(allStringProm);
	}).then((allReward) => {

		var j = 0;
		allReward.forEach((stringsReward) => {
			var description, target, terms;
			if (typeof stringsReward !== 'number') {
				description = stringsReward.child('description').val()
				target = stringsReward.child('target').val()
				terms = stringsReward.child('terms').val()
			}
			else {
				var stringSaved = allReward[stringsReward];
				description = stringSaved.child('description').val()
				target = stringSaved.child('target').val()
				terms = stringSaved.child('terms').val()
			}

			restaurant['rewards'][j]['description'] = description;
			restaurant['rewards'][j]['target'] = target;
			restaurant['rewards'][j]['terms'] = terms;
			j++;
		});
		delete restaurant.lang

		return restaurant
	});
}

// ritorna i ristoranti che si trovano entro i bounds definiti o quelli che 
// matchano subito
exports.onSearch = functions.https.onCall((data, context) => {

	const textQuery = data.textQuery;
	const lang = data.lang.toUpperCase();

	const minLat = data.minLat;
	const minLng = data.minLng;
	const maxLat = data.maxLat;
	const maxLng = data.maxLng;
	const centerLat = data.centerLat;
	const centerLng = data.centerLng;

	const distance = data.distance; 
	const orderBy = data.orderBy;

	// controlla attributi.
	if (!(typeof textQuery === 'string') || textQuery.length === 0 || 
		!(typeof lang === 'string') || lang.length === 0 ||
		!(typeof minLat === 'number') || !(typeof maxLat === 'number') ||
		!(typeof minLng === 'number') || !(typeof maxLng === 'number') || 
		!(typeof centerLat === 'number') || !(typeof centerLng === 'number') || 
		!(typeof distance === 'number') || (orderBy !== 0 && orderBy !== 1))
		// Throwing an HttpsError.
		throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
			'textQuery, lang, minLat, minLng, maxLat, maxLng, centerLat, centerLng, distance, orderBy');

	var firestore = admin.firestore();
	let ref = firestore.collection('coordinates');

	var restaurants = [];
	return directMatch(ref, 'game', textQuery.toLowerCase(), lang).then((response) => {
		restaurants = response;
		return directMatch(ref, 'restaurant', textQuery.toLowerCase(), lang)
	}).then((response) => {
		restaurants = restaurants.concat(response);

		// distance è -1 quando la query forward geocode lato client non ha riscontranto nessun match
		if (restaurants.length > 0 || distance === -1) 
			return { result: restaurants };
		else
			return search(lang, centerLat, centerLng, distance, orderBy + 1, 25, minLat, minLng, maxLat, maxLng);
	});
});

function directMatch(ref, type, query, lang) {
	var allLat = [];
	var allLng = [];
	var allResponse = [];

	return ref.where(type, '==', query).get().then((snapshot) => {
		if (snapshot.empty) {
			return null;
		}

		const promise = [];
		snapshot.forEach((docSnap) => {
			var data = docSnap.data().d;
			var prom = data.ref.get();
			allLat.push(data.latitude);
			allLng.push(data.longitude);
			promise.push(prom);
		});
		return Promise.all(promise);
	}).then((allProm) => {
		var allStringProm = [];
		if (allProm !== null) {
			var i = 0;
			allProm.forEach((snap) => {
				var response = snap.data();
				response['lat'] = allLat[i] 
				response['lng'] = allLng[i] 

				var newLang = lang;
				if (!response['lang'].includes(lang))
					newLang = 'EN'

				var stringProm = admin.database().ref('strings/'+newLang+'/'+response['url']+'_desc').once('value');

				response['lang'] = newLang;
				allResponse.push(response);
				allStringProm.push(stringProm);
				i++;
			});
		}
		return Promise.all(allStringProm);
	}).then((array) => {
		return handleLang(array, allResponse);
	});
}

function handleLang(array0, allResponse) {
	var collection = admin.firestore().collection('reward');
	var allQueryReward = [];
	var restaurants = [];

	var i0 = 0
	array0.forEach((stringSnap) => {
		var restaurant = allResponse[i0]
		restaurant['description'] = stringSnap.val();
		restaurants.push(restaurant);

		var queryReward = collection.where("url", "==", restaurant['url'])
			.where("restaurantId", "==", restaurant['id'])
			.where("enable", "==", true).get();
		allQueryReward.push(queryReward);
		i0++;
	});

	return Promise.all(allQueryReward).then((array) => {
		var i = 0
		var allDocProm = [];

		var savedProm = {}
		array.forEach((querySnapshot) => {
			restaurants[i]['rewards'] = [];
			var allStringProm = [];
			var indexProm = 0;
			querySnapshot.forEach((docSnap) => {
				var data = docSnap.data()
				delete data.enable;
				delete data.goal;
				delete data.publicKey;
				delete data.validityTime;
				delete data.users;

				restaurants[i]['rewards'].push(data);

				var type = data.type
				if (data.extra !== null) 
					type += data.extra
				if (savedProm[type] === undefined) {
					savedProm[type] = [i, indexProm];
					var ref = admin.database().ref('strings/'+restaurants[i]['lang']+'/reward_'+type);
					allStringProm.push(ref.once('value'))
				}
				else {
					allStringProm.push(savedProm[type])
				}

				indexProm++;
			});
			allDocProm.push(Promise.all(allStringProm))
			i++;
		});
		return Promise.all(allDocProm);
	}).then((allRestaurant) => {

		var i = 0
		allRestaurant.forEach((allReward) => {
			var j = 0;
			allReward.forEach((stringsReward) => {
				var description, target, terms;
				if (!Array.isArray(stringsReward)) {
					description = stringsReward.child('description').val()
					target = stringsReward.child('target').val()
					terms = stringsReward.child('terms').val()
				}
				else {
					var stringSaved = allRestaurant[stringsReward[0]][stringsReward[1]];
					description = stringSaved.child('description').val()
					target = stringSaved.child('target').val()
					terms = stringSaved.child('terms').val()
				}

				restaurants[i]['rewards'][j]['description'] = description;
				restaurants[i]['rewards'][j]['target'] = target;
				restaurants[i]['rewards'][j]['terms'] = terms;
				j++;
			});
			delete restaurants[i].lang
			i++;
		});

		return restaurants
	});
}

function search(lang, lat, lng, distance, orderBy, limit, minLat = -1, minLng= -1, maxLat= -1, maxLng=-1) {
	var firestore = admin.firestore();

	var collection = firestore.collection('coordinates');

	const geoQuery = new GeoQuery(collection)

	var allLat = [];
	var allLng = [];

	var allResponse = [];
	// Create a GeoQuery based on a location
	var query = geoQuery
	.near({ center: new admin.firestore.GeoPoint(lat, lng), radius: distance })
	// Get query (as Promise)
	var newQuery = query;

	if (limit !== -1) {
		newQuery = query.limit(limit);
	}

	return newQuery.get().then((value) => {
		const promise = [];
		// All GeoDocument returned by GeoQuery, like the GeoDocument added above
		var sorted = value;

		// distanza ordine crescente, rilevanza e premio ordine decrescente
		if (orderBy === 0)
			sorted = value.docs.sort((a, b) => b.distance - a.distance);
		else if (orderBy === 1)
			sorted = value.docs.sort((a, b) => a.data().relevance - b.data().relevance);
		else if (orderBy === 2)
			sorted = value.docs.sort((a, b) => a.data().reward - b.data().reward);

		sorted.forEach((docSnap) => {		
			var lat = docSnap.data().latitude;
			var lng = docSnap.data().longitude;
			if (minLat === -1) {
				allLat.push(lat);
				allLng.push(lng);
				var prom = docSnap.data().ref.get();
				promise.push(prom);
			}
			else {
				if (lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng) {
					allLat.push(lat);
					allLng.push(lng);
					var prom1 = docSnap.data().ref.get();
					promise.push(prom1);
				}
			}
		})
		return Promise.all(promise);
	}).then((allProm) => {
		var allStringProm = [];
		if (allProm !== null) {
			var i = 0;
			allProm.forEach((snap) => {
				var response = snap.data();
				response['lat'] = allLat[i] 
				response['lng'] = allLng[i] 

				var newLang = lang;
				if (!response['lang'].includes(lang))
					newLang = 'EN'

				var stringProm = admin.database().ref('strings/'+newLang+'/'+response['url']+'_desc').once('value');

				response['lang'] = newLang;
				allResponse.push(response);
				allStringProm.push(stringProm);
				i++;
			});
		}
		return Promise.all(allStringProm)
	}).then((array) => {
		return handleLang(array, allResponse); 
	}).then((restaurants) => {
		return {result : restaurants};
	});
}

exports.createAccount = functions.auth.user().onCreate((userRecord, context) => {
	console.log("nuovo user. uid : " + userRecord.uid);

	return admin.database().ref("/users/"+userRecord.uid+"/data_registrazione").set(admin.database.ServerValue.TIMESTAMP).then(() => {
		var providerId = userRecord.providerData[0].providerId;
		if (providerId === "facebook.com") {
			var nickname = userRecord.displayName
			// Test for the existence of certain keys within a DataSnapshot
			var ref = admin.database().ref("/nickname/"+nickname);
			return ref.once("value").then((snapshot) => {
				if(!snapshot.exists()) {
					var dbRef = admin.database().ref('/nickname/'+nickname);
					return dbRef.set(userRecord.uid).then(() => {
						var dbRef2 = admin.database().ref('/users/'+userRecord.uid+'/nickname');
						return dbRef2.set(nickname);
					});
				}
				else
					return null;
			}).then((nick) => {
				return admin.auth().updateUser(userRecord.uid, {
					emailVerified: true,
					displayName: nick,
				}).then((userRecord) => {
				    // See the UserRecord reference doc for the contents of userRecord.
				    return console.log('Successfully updated user', userRecord.toJSON());
				})
				.catch((error) => {
					return console.log('Error updating user:', error);
				});
			});
		}
		return new Promise((resolve, reject) => {
			resolve(0);
		});
	});
});

exports.onChangeNick = functions.https.onCall((data, context) => {

	const nick = data.nickname;
	// controlla attributi.
	if (nick.length === 0 || nick === null) {
	  // Throwing an HttpsError.
	  throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
	      'one argument: nickname[String] not empty');
	}
	// controlla se l'utente è autenticato.
	if (!context.auth) {
	  // Throwing an HttpsError.
	  throw new functions.https.HttpsError('failed-precondition', 'The function must be called ' +
	      'while authenticated with your user.');
	}

	var refNickname = admin.database().ref('/nickname/'+nick);
	return refNickname.once('value').then((dataSnapshot) => { 
		if (!dataSnapshot.exists()) {
			return handleNewNick(nick, context.auth.uid, refNickname);
		}
		else
			return { text: false } 
	});

});

function handleNewNick(nick, uid, refNickname) {
	var oldNick = null;
	return refNickname.set(uid).then(() => {
		var refUserNick = admin.database().ref('/users/'+uid+'/nickname');
		return refUserNick.once('value');
	}).then((dataSnapshot) => { 
		if (dataSnapshot.exists()) {
			oldNick = dataSnapshot.val();
		}
		return dataSnapshot.ref.set(nick)
	}).then(() => { 
		if (oldNick !== null) {
			return changeOldNick(nick, oldNick, uid);
		}
		return new Promise((resolve, reject) => {
			resolve(0);
		});
	}).then(() => {
		return admin.auth().updateUser(uid, {
			displayName: nick,
		});
	}).then((userRecord) => {
		// See the UserRecord reference doc for the contents of userRecord.
		console.log('Successfully updated user', userRecord.toJSON());
		return { text: true } 
	}).catch((error) => {
		return console.log('Error updating user:', error);
	})
}

function changeOldNick(nick, oldNick, uid) {
	var refOldNick = admin.database().ref('/nickname/'+oldNick);
	return remove(refOldNick).then(() => { 
		var refGames = admin.database().ref('users/'+uid+'/games');
		return refGames.once('value');
	}).then((snapshot) => {
		var promise = [];
	    snapshot.forEach((games) => {
	    	var game = games.key;
	    	var prom = games.ref.once('value').then((scoreGame) => {
				var promise2 = [];
			    scoreGame.forEach((singleScore) => { 
			    	var type = singleScore.key;
			    	var score = singleScore.val();
					var refRankScore = admin.database().ref('/rankings/'+game+'/'+type+'/'+score+'/'+uid+'/user');
			    	var prom2 = refRankScore.set(nick);
			    	promise2.push(prom2);

					var refRank = admin.database().ref('/rankings/'+game+'/'+type);
					var i = 7;
					if (type === 'global') 
						i = 3;
			    	var prom3 = getRank(refRank, i, type+'Crc32', false);
			    	promise2.push(prom3);
			    });
			    return Promise.all(promise2);
	    	});
	    	promise.push(prom);
	  	});
	  	return Promise.all(promise);
	})
}

exports.onGetRankPosition = functions.https.onCall((data, context) => {
	
	var user;
	if (context.auth !== undefined) {
		user = context.auth.uid;
	}
	const game = data.game;

	// controlla attributi.
	if ((typeof game !== 'string') || game.length === 0) {
		// Throwing an HttpsError.
		throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
			'game[String]');
	}

	var numberWeek;

	const refNumberWeekly = admin.database().ref('/game/'+game+'/week');
	return refNumberWeekly.once('value').then((snapNumberWeek) => {
		numberWeek = snapNumberWeek.val();
		return returnRank(game, numberWeek, null, null, true, true);
	}).then((rankings) => {
		if (user === null) 
			return rankings;
		else
			return getPositionUser(user, game, numberWeek, rankings)
	});
})

function getPositionUser(user, game, numberWeek, rankings) {
	var allProm = [];
	const weekly = 'weekly'+numberWeek;

	const refGameRank = admin.database().ref('/rankings/'+game);
	const refUserGameRank = admin.database().ref('/users/'+user+'/games/'+game);
	var globalScore = null;
	var weeklyScore = null;
	
	return refUserGameRank.once('value').then((snapUserGameRank) => {
		const value = snapUserGameRank.val();

		if (value !== null) {
			if (value.global !== undefined) {
				globalScore = value.global;
				allProm.push(countPositionInScore(refGameRank, globalScore, user, 'global'));
			}
			if (value[weekly] !== undefined) {
				weeklyScore = value[weekly];
				allProm.push(countPositionInScore(refGameRank, weeklyScore, user, weekly));
			}
		}
		return Promise.all(allProm);
	}).then((result) => {
		const score = [globalScore, weeklyScore]
		const types = ['global', 'weekly']
		var i = 0;
		result.forEach((count) => {
			rankings[types[i]]['user'] = {};
			rankings[types[i]]['user']['position'] = count;
			rankings[types[i]]['user']['score'] = score[i];
			i++;
		})
		return rankings;
	});
}

function countPositionInScore(refGameRank, score, user, type) {
	var countUser;
	const ref = refGameRank.child(type);
	const refScore = ref.child(score);
	return refScore.child(user).child('index').once('value').then((snap) => {
		const index = snap.val();
		return refScore.orderByChild('index').endAt(index).once('value');
	}).then((snapshot) => {
		countUser = snapshot.numChildren();
		return ref.orderByKey().startAt((score + 1).toString()).once('value');
	}).then((snapAllPosition) => {
		var i = 0;
		snapAllPosition.forEach((snapPos) => {
			i += snapPos.numChildren() 
		})
		return i+countUser;
	})
}

exports.onCreateSession = functions.https.onCall((data, context) => {

	const user = context.auth.uid;
	const game = data.game;
	const score = data.score;
	// controlla attributi.
	if ((!(typeof user === 'string') || user.length === 0) || 
		(!(typeof game === 'string') || game.length === 0) || 
		!(typeof score === 'number')) {
	  // Throwing an HttpsError.
	  throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
	      'three argument: user[String], game[String], score[number]');
	}
	// controlla se l'utente è autenticato ed è la sua identità.
	if (!context.auth) {
	  // Throwing an HttpsError.
	  throw new functions.https.HttpsError('failed-precondition', 'The function must be called ' +
	      'while authenticated with your user.');
	}
	checkRewardEarned(user, game, score)

	return handleSession(user, game, score);
});
	
function handleSession(user, game, score) {
	var refOldScore = admin.database().ref('/users/'+user+'/games/'+game);
	return refOldScore.once('value').then((dataSnapshot) => {
		// estrai il vecchio punteggio global
		var oldScoreGlobal = dataSnapshot.child('global').val();
		// estrai il numero week
		var refNumberWeekly = admin.database().ref('/game/'+game+'/week');
		return refNumberWeekly.once('value').then((snapNumberWeek) => {
			const numberWeek = snapNumberWeek.val();
			// estrai il vecchio punteggio weekly
			var oldScoreWeekly = dataSnapshot.child('weekly'+numberWeek).val();

			// se il vecchio globalScore è null
			if (oldScoreGlobal === null) {
				// imposta il weeklyScore e GlobalScore
				return setScore(user, game, score, score, true, dataSnapshot.ref, numberWeek).then(() => {
					return returnRank(game, numberWeek, null, null, true, false);
				});
			}

			// se il vecchio weeklyScore è null oppure lo score di sessione lo batte
			if (oldScoreWeekly === null || score > oldScoreWeekly) {
				// controlla se lo score di sessione supera anche il globalScore
				var setGlobal = false;
				var globalScore = oldScoreGlobal;
				var refOldGlobalScore = null
				var refOldWeeklyScore = null

				if (score > oldScoreGlobal) {
					refOldGlobalScore = admin.database().ref('/rankings/'+game+'/global/'+oldScoreGlobal+'/'+user);
					setGlobal = true;
					globalScore = score;
				}

				if (oldScoreWeekly !== null && score > oldScoreWeekly) {
					// rimuovi il vecchio posto in classifica weekly
					refOldWeeklyScore = admin.database().ref('/rankings/'+game+'/weekly'+numberWeek+'/'+oldScoreWeekly+'/'+user);

				}

				// imposta il nuovo weeklyScore e, sulla base del controllo fatto prima, il globalScore
				return remove(refOldWeeklyScore).then(() => {
					return remove(refOldGlobalScore).then(() => {
						// imposta il nuovo score
						return setScore(user, game, score, globalScore, setGlobal, dataSnapshot.ref, numberWeek).then(() => {
							// ritorna classifica settimanale e globale
							return returnRank(game, numberWeek, null, null, true, false);
						});
					})
				})
			}
			// in tutti gli altri casi ritorna i vecchi score
			return returnRank(game, numberWeek, null, null, true, false);
		});
	});	
}

// rimuovi la referenzaDB
function remove(ref) {
	if (ref !== null) {
		return ref.remove().then(() => {
		    return console.log("Nodo rimosso."+ref.toString())
		})
		.catch((error) => {
		   	return console.log("Errore: " + error.message)
		});
	}
	return new Promise((resolve, reject) => {
		resolve(0);
	});
}

// imposta i nuovi score
function setScore(user, game, score, globalScore, setGlobal, refScore, numberWeek) {
	week = "weekly"+numberWeek
	var obj = {};
	obj[week] = score;
	obj['global'] = globalScore;
	// estrai nickname
	var refNickname = admin.database().ref('/users/'+user+'/nickname');
	return refNickname.once("value").then((nickname) => {
		// aggiorna i punteggi
		return refScore.set(obj).then(() => {
			// prendi referenza in score e prendi l'utente con più alto indice
			var refRankWeeklyScoreExisted = admin.database().ref('/rankings/'+game+'/'+week+'/'+score);
			return refRankWeeklyScoreExisted.orderByChild('index').limitToLast(1).once("value").then((snapshot0) => {
				var index = 1;
				if (snapshot0.val() !== null) {
					snapshot0.forEach((childSnapshot0) => {
						index = childSnapshot0.child('index').val() + 1;
					});
				}
				var obj2 = {};
				obj2['user'] = nickname.val();
				obj2['index'] = index;
				var refRankWeeklyScore = refRankWeeklyScoreExisted.child(user)
				// imposta posto in classifica settimanale
				return refRankWeeklyScore.set(obj2).then(() => {
					if (setGlobal) {
						var refRankGlobalScoreExisted = admin.database().ref('/rankings/'+game+'/global/'+globalScore);
						return refRankGlobalScoreExisted.orderByChild('index').limitToLast(1).once("value").then((snapshot1) => {
							var index2 = 1;
							if (snapshot1.val() !== null) {
								snapshot1.forEach((childSnapshot1) => {
									index2 = childSnapshot1.child('index').val() + 1;
								});
							}
							obj2['index'] = index2;
							// imposta posto in classifica globale se è stato battuto il record
							var refRankGlobalScore = refRankGlobalScoreExisted.child(user)
							return refRankGlobalScore.set(obj2).then(() => {
								return console.log("Nodo classifica aggiunto [global, " + week + "]: " + game + "/" + score + "/" + user);
							});
						});
					}
					return console.log("Nodo classifica aggiunto [" + week + "]: " + game + "/" + score + "/" + user);
				})
				.catch((error) => {
					return console.log("Errore: " + error.message);
				});
			});
		});
	});
}

exports.onGetRank = functions.https.onCall((data, context) => {

	const user = context.auth.uid;
	const score = data.score;
	const crc32GlobalSaved = data.crc32Global;
	const crc32WeeklySaved = data.crc32Weekly;
	const game = data.game;
	// controlla attributi.
	if ((typeof game !== 'string') || game.length === 0) {
		// Throwing an HttpsError.
		throw new functions.https.HttpsError('invalid-argument', 'The function must be called with ' +
			'three argument: user[String], game[String], score[number]');
	}
	// controlla se l'utente è autenticato.
	if (!context.auth) {
		// Throwing an HttpsError.
		throw new functions.https.HttpsError('failed-precondition', 'The function must be called ' +
			'while authenticated with your user.');
	}

	checkRewardEarned(user, game, score)

	var refNumberWeekly = admin.database().ref('/game/'+game+'/week');
	return refNumberWeekly.once('value').then((snapNumberWeek) => {
		const numberWeek = snapNumberWeek.val();
		return returnRank(game, numberWeek, crc32GlobalSaved, crc32WeeklySaved, false, false);
	});
});

// ritorna classifiche
function returnRank(game, numberWeek, crc32GlobalSaved, crc32WeeklySaved, force, getPhoto) {
	var refRank = admin.database().ref('/rankings/'+game);
	var refRankGlobalCrc32 = refRank.child('globalCrc32');
	var refRankWeeklyCrc32 = refRank.child('weekly'+numberWeek+'Crc32');

	// ritorna ultimi x elementi in ordine crescente
	var rankGlobal = null;

	var getGlobal = false;
	var getWeekly = false;

	return refRankGlobalCrc32.once("value").then((snapcrc32global) => { 
		var crc32GlobalExisted = snapcrc32global.val();
		if ((crc32GlobalExisted !== null) && ((crc32GlobalSaved !== crc32GlobalExisted)) || force) {
			getGlobal = true;
		}
		return refRankWeeklyCrc32.once("value")
	}).then((snapcrc32weekly) => { 
		var crc32WeeklyExisted = snapcrc32weekly.val();
		if ((crc32WeeklyExisted !== null) && ((crc32WeeklySaved !== crc32WeeklyExisted)) || force) {
			getWeekly = true;
		}

		if (getGlobal){
			var refRankGlobal = refRank.child('global');
			return getRank(refRankGlobal, 3, 'globalCrc32', getPhoto);
		}
		else
			return null
	}).then((rankGlobalTemp) => {
		rankGlobal = rankGlobalTemp;
		if (getWeekly) {
			var refRankWeekly = refRank.child('weekly'+numberWeek);
			return getRank(refRankWeekly, 7, 'weekly'+numberWeek+'Crc32', getPhoto);
		}
		else
			return null;
	}).then((rankWeekly) => { 
		return { global: rankGlobal, weekly: rankWeekly };
	});
}

function getRank(refRank, i, crcKey, getPhoto) {
	var num = 0;
	var crc32 = "";
	var list = [];
	var ordered = [];
	return refRank.orderByKey().limitToLast(i).once("value").then((snapshot0) => {
		ordered = desc(snapshot0);
		var totalProm= [];
		ordered.forEach((score) => {
			var prom = refRank.child(score).orderByChild('index').once("value")
			totalProm.push(prom);
		});
		return Promise.all(totalProm);
	}).then((allProm) => {
		var allPhotoProm = [];

		var exit = false;
		var index = 0;
		allProm.forEach((snapscore) => {
				if (exit) {
					return;
				}
				snapscore.forEach((singlescore) => {
					if (num < i) {
						crc32 += ordered[index] + singlescore.key + singlescore.child('user').val() + singlescore.child('index').val();
						var newElement = [singlescore.child('user').val(), ordered[index]]
						list.push(newElement);
						if (getPhoto) {
							allPhotoProm.push(admin.auth().getUser(singlescore.key))
						}
						num++;
					}
					else {
						exit = true;
						return;
					}
				});
				index ++;
		});
		return Promise.all(allPhotoProm);
	}).then((allPhotoProm) => {
		var obj = {};
		var crc32Done = CRC32(crc32);
		refRank.parent.child(crcKey).set(crc32Done)

		obj['crc32'] = crc32Done;

		index = 0;
		allPhotoProm.forEach((user) => {
			list[index].push(user.photoURL);
			index++;
		})
			
		obj['rank'] = list;
		return obj;
	});
}

function desc(snap) {
	orderedDesc = [];
	snap.forEach((childSnap) => {
		orderedDesc.unshift(childSnap.key);
	});
	return orderedDesc;
}

// algoritmo di checksum CRC32. fonte: https://helloacm.com/crc32-calculator-javascript/
var CRC32 = function(str){
	var CRCTable = [
		0x00000000,0x77073096,0xEE0E612C,0x990951BA,0x076DC419,0x706AF48F,0xE963A535,0x9E6495A3,
		0x0EDB8832,0x79DCB8A4,0xE0D5E91E,0x97D2D988,0x09B64C2B,0x7EB17CBD,0xE7B82D07,0x90BF1D91,
		0x1DB71064,0x6AB020F2,0xF3B97148,0x84BE41DE,0x1ADAD47D,0x6DDDE4EB,0xF4D4B551,0x83D385C7,
		0x136C9856,0x646BA8C0,0xFD62F97A,0x8A65C9EC,0x14015C4F,0x63066CD9,0xFA0F3D63,0x8D080DF5,
		0x3B6E20C8,0x4C69105E,0xD56041E4,0xA2677172,0x3C03E4D1,0x4B04D447,0xD20D85FD,0xA50AB56B,
		0x35B5A8FA,0x42B2986C,0xDBBBC9D6,0xACBCF940,0x32D86CE3,0x45DF5C75,0xDCD60DCF,0xABD13D59,
		0x26D930AC,0x51DE003A,0xC8D75180,0xBFD06116,0x21B4F4B5,0x56B3C423,0xCFBA9599,0xB8BDA50F,
		0x2802B89E,0x5F058808,0xC60CD9B2,0xB10BE924,0x2F6F7C87,0x58684C11,0xC1611DAB,0xB6662D3D,
		0x76DC4190,0x01DB7106,0x98D220BC,0xEFD5102A,0x71B18589,0x06B6B51F,0x9FBFE4A5,0xE8B8D433,
		0x7807C9A2,0x0F00F934,0x9609A88E,0xE10E9818,0x7F6A0DBB,0x086D3D2D,0x91646C97,0xE6635C01,
		0x6B6B51F4,0x1C6C6162,0x856530D8,0xF262004E,0x6C0695ED,0x1B01A57B,0x8208F4C1,0xF50FC457,
		0x65B0D9C6,0x12B7E950,0x8BBEB8EA,0xFCB9887C,0x62DD1DDF,0x15DA2D49,0x8CD37CF3,0xFBD44C65,
		0x4DB26158,0x3AB551CE,0xA3BC0074,0xD4BB30E2,0x4ADFA541,0x3DD895D7,0xA4D1C46D,0xD3D6F4FB,
		0x4369E96A,0x346ED9FC,0xAD678846,0xDA60B8D0,0x44042D73,0x33031DE5,0xAA0A4C5F,0xDD0D7CC9,
		0x5005713C,0x270241AA,0xBE0B1010,0xC90C2086,0x5768B525,0x206F85B3,0xB966D409,0xCE61E49F,
		0x5EDEF90E,0x29D9C998,0xB0D09822,0xC7D7A8B4,0x59B33D17,0x2EB40D81,0xB7BD5C3B,0xC0BA6CAD,
		0xEDB88320,0x9ABFB3B6,0x03B6E20C,0x74B1D29A,0xEAD54739,0x9DD277AF,0x04DB2615,0x73DC1683,
		0xE3630B12,0x94643B84,0x0D6D6A3E,0x7A6A5AA8,0xE40ECF0B,0x9309FF9D,0x0A00AE27,0x7D079EB1,
		0xF00F9344,0x8708A3D2,0x1E01F268,0x6906C2FE,0xF762575D,0x806567CB,0x196C3671,0x6E6B06E7,
		0xFED41B76,0x89D32BE0,0x10DA7A5A,0x67DD4ACC,0xF9B9DF6F,0x8EBEEFF9,0x17B7BE43,0x60B08ED5,
		0xD6D6A3E8,0xA1D1937E,0x38D8C2C4,0x4FDFF252,0xD1BB67F1,0xA6BC5767,0x3FB506DD,0x48B2364B,
		0xD80D2BDA,0xAF0A1B4C,0x36034AF6,0x41047A60,0xDF60EFC3,0xA867DF55,0x316E8EEF,0x4669BE79,
		0xCB61B38C,0xBC66831A,0x256FD2A0,0x5268E236,0xCC0C7795,0xBB0B4703,0x220216B9,0x5505262F,
		0xC5BA3BBE,0xB2BD0B28,0x2BB45A92,0x5CB36A04,0xC2D7FFA7,0xB5D0CF31,0x2CD99E8B,0x5BDEAE1D,
		0x9B64C2B0,0xEC63F226,0x756AA39C,0x026D930A,0x9C0906A9,0xEB0E363F,0x72076785,0x05005713,
		0x95BF4A82,0xE2B87A14,0x7BB12BAE,0x0CB61B38,0x92D28E9B,0xE5D5BE0D,0x7CDCEFB7,0x0BDBDF21,
		0x86D3D2D4,0xF1D4E242,0x68DDB3F8,0x1FDA836E,0x81BE16CD,0xF6B9265B,0x6FB077E1,0x18B74777,
		0x88085AE6,0xFF0F6A70,0x66063BCA,0x11010B5C,0x8F659EFF,0xF862AE69,0x616BFFD3,0x166CCF45,
		0xA00AE278,0xD70DD2EE,0x4E048354,0x3903B3C2,0xA7672661,0xD06016F7,0x4969474D,0x3E6E77DB,
		0xAED16A4A,0xD9D65ADC,0x40DF0B66,0x37D83BF0,0xA9BCAE53,0xDEBB9EC5,0x47B2CF7F,0x30B5FFE9,
		0xBDBDF21C,0xCABAC28A,0x53B39330,0x24B4A3A6,0xBAD03605,0xCDD70693,0x54DE5729,0x23D967BF,
		0xB3667A2E,0xC4614AB8,0x5D681B02,0x2A6F2B94,0xB40BBE37,0xC30C8EA1,0x5A05DF1B,0x2D02EF8D
	];
	var len = str.length;
	var r = 0xffffffff;
	for (var i = 0; i < len; i ++) {        
		r = (r >> 8) ^ CRCTable[str[i] ^ (r & 0x000000FF)];
	}
	return ~r;
}
