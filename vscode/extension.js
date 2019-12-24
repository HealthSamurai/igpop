const path = require("path");
const vscode = require('vscode');
const lsp = require('vscode-languageclient');
const net = require('net');

function activate(context) {
    // let serverOptions = {
    //     run: {
    //         port: 7345,
    //         transport: lsp.TransportKind.socket,
    //         command: "bash",
    //         args: ["-c", "sleep 10000"]
    //     },
    //     debug: {
    //         port: 7345,
    //         transport: lsp.TransportKind.socket,
    //         command: "bash",
    //         args: ["-c", "sleep 10000"]
    //         // options: debugOptions
    //     }
    // };

        server = context.asAbsolutePath(path.join('server.js'));

        let serverOptions = function(args){
            console.log("Here", args); 
            return new Promise(function(resolve, reject){
                var client = new net.Socket();
                client.connect(7345, "127.0.0.1", function() {
                   console.log('connected');
                   resolve({
                        reader: client,
                        writer: client
                    });
                  })
            });
			
        };

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
