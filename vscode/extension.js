const vscode = require('vscode');
const lsp = require('vscode-languageclient');
const net = require('net');
const { spawn } = require('child_process');

function getPath() {
    return vscode.workspace.getConfiguration('igpoplsp').get('path');
}

function activate(context) {
  let server;
  let client;
  const srv = net.createServer(socket => {
    console.log('Process connected');
    socket.on('end', () => {
      console.log('Process disconnected');
    });
    srv.close();
    resolve({ reader: socket, writer: socket });
  });
  // Listen on random port
  let createAndRunServer = function(){
    return new Promise((resolve, reject) => {
      let port;
      srv.listen(0, '127.0.0.1', () => {
        console.log('Spawning igpop Language Server...')
        const port = srv.address().port;
        var path = getPath();
        var args = ["lsp", "-p", port];
        srv.close(() => {
          server = spawn(path, args, {cwd: vscode.workspace.rootPath} );
          resolve(port);
          server.stderr.on('data', chunk => {
            const str = chunk.toString();
            console.log('igpop Language Server:', str);
            client.outputChannel.appendLine(str);
          });
          server.on('exit', (code, signal) => {
            if (code !== 0) {
              console.log(`igpop Language Server exited ` + (signal ? `from signal ${signal}` : `with exit code ${code}`));
            }
          })
          console.log(`igpop Language Server process spawned on port ${port}`)
        });
      });
    });
  };
  let serverOptions = (args => {
    return new Promise((resolve, reject) => {
      let lspserver
      let connect = (client, port) => {
        client.connect(port, "127.0.0.1", () => {
          console.log('connected');
          resolve({
            reader: client,
            writer: client
          });
        });
      }
      client = new net.Socket();
      lspserver = createAndRunServer();
      lspserver.then((port) => {
        connect(client, port);
        client.on('error', () => {
          console.log('socket closed, try reconnect');
          let reconnectLoop = setInterval(() => {
            try {
              client.end();
            } finally {}
            connect(client, port)
            clearInterval(reconnectLoop);
          }, 5000);
        });
      });
    });
  });

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
  client = new lsp.LanguageClient('igpop', 'Language Server Igpop', serverOptions, clientOptions);
  client.start();
  context.subscriptions.push(client);

  let disposable = vscode.commands.registerCommand('extension.igpop-lsp', function () {
    vscode.window.showInformationMessage('Enable igpop LSP extension');
  });

  context.subscriptions.push(disposable);
}
exports.activate = activate;
function deactivate() {
  console.log("Deactivated Extension");
  server.kill();
  if (!client) {
    return undefined;
  }
  return client.stop();
}
exports.deactivate = deactivate;
