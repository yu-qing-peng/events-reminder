const cloud = require('wx-server-sdk');

cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV });

const db = cloud.database();
const _ = db.command;

async function addEvent(userId, title, description, eventDate) {
  try {
    const result = await db.collection('events').add({
      data: {
        userId: userId,
        title: title,
        description: description || '',
        eventDate: eventDate,
        reminded: false,
        createdAt: new Date()
      }
    });
    return { success: true, id: result._id };
  } catch (err) {
    console.error('Add event error:', err);
    return { success: false, error: 'Failed to create event' };
  }
}

async function getEvents(userId) {
  try {
    const events = await db.collection('events').where({
      userId: userId
    }).orderBy('eventDate', 'asc').get();
    return { success: true, events: events.data };
  } catch (err) {
    console.error('Get events error:', err);
    return { success: false, error: 'Failed to fetch events' };
  }
}

async function deleteEvent(eventId) {
  try {
    await db.collection('events').doc(eventId).remove();
    return { success: true };
  } catch (err) {
    console.error('Delete event error:', err);
    return { success: false, error: 'Failed to delete event' };
  }
}

async function markReminded(eventId) {
  try {
    await db.collection('events').doc(eventId).update({
      data: { reminded: true }
    });
    return { success: true };
  } catch (err) {
    console.error('Mark reminded error:', err);
    return { success: false };
  }
}

exports.main = async (event, context) => {
  const { action, userId, title, description, eventDate, eventId } = event;

  switch (action) {
    case 'add':
      return await addEvent(userId, title, description, eventDate);
    case 'get':
      return await getEvents(userId);
    case 'delete':
      return await deleteEvent(eventId);
    case 'markReminded':
      return await markReminded(eventId);
    default:
      return { success: false, error: 'Invalid action' };
  }
};