const cloud = require('wx-server-sdk');
const bcrypt = require('bcryptjs');

cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV });

const db = cloud.database();
const _ = db.command;

async function login(username, password) {
  if (!username || !password) {
    return { success: false, error: 'Missing username or password' };
  }

  try {
    const users = await db.collection('users').where({
      username: username
    }).get();

    if (users.data.length === 0) {
      return { success: false, error: 'Invalid credentials' };
    }

    const user = users.data[0];
    const valid = await bcrypt.compare(password, user.password);

    if (!valid) {
      return { success: false, error: 'Invalid credentials' };
    }

    return {
      success: true,
      userId: user._id,
      username: user.username
    };
  } catch (err) {
    console.error('Login error:', err);
    return { success: false, error: 'Server error' };
  }
}

exports.main = async (event, context) => {
  const { username, password } = event;
  return await login(username, password);
};