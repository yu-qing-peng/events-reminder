const cloud = require('wx-server-sdk');
const bcrypt = require('bcryptjs');

cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV });

const db = cloud.database();

async function register(username, password) {
  if (!username || !password) {
    return { success: false, error: 'Missing username or password' };
  }

  try {
    const existing = await db.collection('users').where({
      username: username
    }).get();

    if (existing.data.length > 0) {
      return { success: false, error: 'Username already exists' };
    }

    const hashed = await bcrypt.hash(password, 10);

    const result = await db.collection('users').add({
      data: {
        username: username,
        password: hashed,
        createdAt: new Date()
      }
    });

    return {
      success: true,
      userId: result._id
    };
  } catch (err) {
    console.error('Register error:', err);
    return { success: false, error: 'Server error' };
  }
}

exports.main = async (event, context) => {
  const { username, password } = event;
  return await register(username, password);
};