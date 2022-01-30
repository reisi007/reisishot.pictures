import 'dotenv/config' // see https://github.com/motdotla/dotenv#how-do-i-use-dotenv-with-import
import {deploy, excludeDefaults} from "@samkirkland/ftp-deploy";

const env = process.env;

const host = env["ftp_host"];
const user = env["ftp_user"]
const password = env["ftp_password"]

async function deployMyCode(domain) {
    console.log("🚚 Deploy started");
    await deploy({
        server: host,
        username: user,
        password: password,
        protocol: "ftps",
        "local-dir": "../upload/" + domain + "/",
        "server-dir": "/" + domain + "/",
        exclude: [...excludeDefaults]
    });
    console.log("🚀 Deploy done!");
}

await deployMyCode("images.reisishot.pictures");
await deployMyCode("static.reisishot.pictures");
await deployMyCode("reisishot.pictures");
