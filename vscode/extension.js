const path = require("path");
const vscode = require('vscode');
const lsp = require('vscode-languageclient');
const net = require('net');
const { spawn } = require('child_process');

function activate(context) {
  let client;
  let tcpServerOptions = () => new Promise((resolve, reject) => {
    const server = net.createServer(socket => {
      console.log('Process connected');
      socket.on('end', () => {
        console.log('Process disconnected');
      });
      server.close();
      resolve({ reader: socket, writer: socket });
    });
    // Listen on random port
    server.listen(0, '127.0.0.1', () => {
      console.log('Starting server')
      const port = (server.address()).port;
      var path = "/usr/bin/java"
      var args = ["-jar", "/home/flawless/projects/igpop/target/igpop.jar",
                  "lsp", "-p", port];
      const childProcess = spawn(path, args, {cwd: '/home/flawless/projects/igpop/example'} );
      console.log('Server spawned')
      childProcess.stderr.on('data', chunk => {
        const str = chunk.toString();
        console.log('Igpop Language Server:', str);
        client.outputChannel.appendLine(str);
      });
      childProcess.on('exit', (code, signal) => {
        client.outputChannel.appendLine(`Language server exited ` + (signal ? `from signal ${signal}` : `with exit code ${code}`));
        if (code !== 0) {
          client.outputChannel.show();
        }
      });
      return childProcess;
    });
  });
    // let serverOptions = {
    //     run: {
    //       port: 7345,
    //       transport: lsp.TransportKind.socket,
    //       command: "java",
    //       args: ["--jar", "/home/flawless/projects/igpop/target/igpop.jar", "lsp"]
    //     },
    //     debug: {
    //         port: 7345,
    //         transport: lsp.TransportKind.socket,
    //         command: "bash",
    //         args: ["-c", "sleep 10000"]
    //         // options: debugOptions
    //     }
    // };


        // let serverOptions = function(args){
        //     return new Promise(function(resolve, reject){
        //         var client = new net.Socket();
        //         var result = {}
        //         client.connect(7345, "127.0.0.1", function() {
        //            console.log('connected');
        //            result.reader = client;
        //            result.reader = client;
        //            resolve({
        //                 reader: client,
        //                 writer: client
        //             });
        //         });
        //         client.on('closed', () => {
        //            console.log('socket closed, try reconnect');
        //            setTimeout(()=>{
        //               try {
        //                   client.end();
        //               } finally {}
        //               client = new net.Socket();
        //               client.connect(7345, "127.0.0.1", function() {
        //                 console.log('Re-connected');
        //                 result.reader = client;
        //                 result.reader = client;
        //               });

        //            }, 2000);

        //         });
        //     });

        // };

  let clientOptions = {
    documentSelector: [{ scheme: 'file', language: 'igpop' }],
    synchronize: {
      // Notify the server about file changes to '.clientrc files contained in the workspace
      configurationSection: 'igpop-lsp',
      fileEvents: [
        vscode.workspace.createFileSystemWatcher('**/*.igpop')
      ]
    }
  };
  client = new lsp.LanguageClient('igpop', 'Language Server Igpop', tcpServerOptions, clientOptions);
  client.start();
  context.subscriptions.push(client);

  console.log('Congratulations, your extension "hello" is now active!');

  let disposable = vscode.commands.registerCommand('extension.helloWorld', function () {
    vscode.window.showInformationMessage('Hello World!');
  });

  context.subscriptions.push(disposable);
}
exports.activate = activate;
function deactivate() {
  if (!client) {
    return undefined;
  }
  return client.stop();
}
exports.deactivate = deactivate;
