const cloud = require('wx-server-sdk');

cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV });

const db = cloud.database();
const _ = db.command;
const $ = db.command.aggregate;

const TEMPLATE_ID = 'YOUR_TEMPLATE_ID';

async function checkAndSendReminders() {
  try {
    const now = new Date();
    const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000);
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);

    const events = await db.collection('events').where({
      reminded: false,
      eventDate: _.and(
        _.gte(now.toISOString()),
        _.lte(oneHourLater.toISOString())
      )
    }).get();

    console.log(`Found ${events.data.length} events to remind`);

    for (const ev of events.data) {
      const evTime = new Date(ev.eventDate);
      const diffMinutes = Math.round((evTime - now) / 60000);

      let message = '';
      if (diffMinutes > 0 && diffMinutes <= 60) {
        message = `Happening in ${diffMinutes} minutes!`;
      } else if (diffMinutes <= 0) {
        message = 'Starting now!';
      } else {
        continue;
      }

      try {
        await db.collection('events').doc(ev._id).update({
          data: { reminded: true }
        });

        console.log(`Marked event ${ev._id} as reminded`);
      } catch (err) {
        console.error('Failed to mark reminded:', err);
      }
    }

    return { success: true, count: events.data.length };
  } catch (err) {
    console.error('Check reminders error:', err);
    return { success: false, error: err.message };
  }
}

exports.main = async (event, context) => {
  return await checkAndSendReminders();
};