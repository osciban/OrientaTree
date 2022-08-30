const functions = require("firebase-functions");

// The Firebase Admin SDK to access Firestore.
const admin = require("firebase-admin");
admin.initializeApp();

// cuando se cree una participacion, se añade un valor al array de participaciones de la actividad
exports.sanitizeParticipationsOnCreate = functions.firestore.document('/activities/{activityID}/participations/{participationID}')
.onCreate((snap, context) => {
  // obtener el id de la participacion que se acaba de crear
  const participation = snap.data().participant;

  // obtener la actividad padre de la participacion, cuyo array hay que actualizar
  const activity = admin.firestore().collection('/activities').doc(context.params.activityID).get();

  return activity.then(oldActivity => {
    // obtenemos el id de la actividad
    const idAct = oldActivity.data().id;
    // obtenemos el array de participaciones de la actividad antes de ser actualizado
    const oldArray = oldActivity.data().participants;
    // modificamos el array, incluyendo el id de la nueva participacion
    oldArray.push(participation);
    // actualizamos el array de la actividad
    admin.firestore().collection('/activities').doc(idAct).update({participants: oldArray});
  }).catch(err => {
    console.log('Error getting document', err);
  });

});

// cuando se elimine una participacion, se elimina tambien el objeto correspondiente del array de la actividad padre
exports.sanitizeParticipationsOnDelete = functions.firestore.document('/activities/{activityID}/participations/{participationID}')
.onDelete((snap, context) => {
  const participation = snap.data().participant;
  const activity = admin.firestore().collection('/activities').doc(context.params.activityID).get();
  return activity.then(oldActivity => {
    const idAct = oldActivity.data().id;
    const oldArray = oldActivity.data().participants;
    const index = oldArray.indexOf(participation);
    if (index > -1) {
      oldArray.splice(index, 1);
    }
    admin.firestore().collection('/activities').doc(idAct).update({participants: oldArray});
  }).catch(err => {
    console.log('Error getting document', err);
  });
});

// elimina el documento de un usuario si eliminamos dicho usuario de firebase Auth
exports.deleteUserDoc = functions.auth.user().onDelete((user) => {
  const userId = user.uid;
  const deleteUser = admin.firestore().collection('users').doc(userId).delete();
  return deleteUser.then(() => {
    console.log('Documento eliminado');
  });
});
