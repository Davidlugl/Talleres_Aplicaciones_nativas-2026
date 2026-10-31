const cloudinary = require('cloudinary').v2;
const config = require('./config');

cloudinary.config(config.cloudinary);

module.exports = cloudinary;
