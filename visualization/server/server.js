
let services= require("./services.js")
let express = require("express");
let serveStatic = require("serve-static");
let history = require("connect-history-api-fallback");
let cors = require('cors');
let app = express();
let dotenv = require('dotenv');
dotenv.config({ path: '../.env' });
const { MongoClient } = require("mongodb");
const uri = "mongodb+srv://" + process.env.MONGO_USERNAME + ":" + process.env.MONGO_PASSWORD + "@cluster0.yidvg.mongodb.net/myFirstDatabase?retryWrites=true&w=majority";
const client = new MongoClient(uri);
app.get('/repo', services.mongodbcall);
app.get('/userdata', services.mongodbcall2);
app.use(history());
app.use(serveStatic(__dirname + "/dist"));
app.get("/", (req, res) => {
  res.status(200).send("This is the home page!");
});
app.get("/visualization", (req, res) => {
  res.status(200).send("This is the stats page!");
});
app.use(cors());
let port = process.env.APP_PORT || 8081;
module.exports = app.listen(port);
