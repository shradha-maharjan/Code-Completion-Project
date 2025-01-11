// // The module 'vscode' contains the VS Code extensibility API
// // Import the module and reference it with the alias vscode in your code below
// const vscode = require('vscode');

// // This method is called when your extension is activated
// // Your extension is activated the very first time the command is executed

// /**
//  * @param {vscode.ExtensionContext} context
//  */
// function activate(context) {

// 	// Use the console to output diagnostic information (console.log) and errors (console.error)
// 	// This line of code will only be executed once when your extension is activated
// 	console.log('Congratulations, your extension "popup-test" is now active!');

// 	// The command has been defined in the package.json file
// 	// Now provide the implementation of the command with  registerCommand
// 	// The commandId parameter must match the command field in package.json
// 	const disposable = vscode.commands.registerCommand('popup-test.helloWorld', function () {
// 		// The code you place here will be executed every time your command is executed

// 		// Display a message box to the user
// 		vscode.window.showInformationMessage('Hello World from Popup test!');
// 	});

// 	context.subscriptions.push(disposable);
// }

// // This method is called when your extension is deactivated
// function deactivate() {}

// module.exports = {
// 	activate,
// 	deactivate
// }

// const vscode = require('vscode');

// /**
//  * @param {vscode.ExtensionContext} context
//  */
// function activate(context) {
//     console.log('Extension "popup-test" is now active!');

//     // Register the command
//     let disposable = vscode.commands.registerCommand('popup-test.helloWorld', function () {
//         // Create and show a new webview panel
//         const panel = vscode.window.createWebviewPanel(
//             'javaEditor', // Identifies the type of the webview
//             'Java Code Editor', // Title of the webview
//             vscode.ViewColumn.One, // Editor column to show the new webview
//             {
//                 enableScripts: true // Allow JavaScript in the webview
//             }
//         );

//         // Set the HTML content of the webview
//         panel.webview.html = getWebviewContent();

// 		// Handle messages from the webview
//         panel.webview.onDidReceiveMessage(
//             message => {
//                 switch (message.command) {
//                     case 'submit':
//                         vscode.window.showInformationMessage('Java Code Submitted!');
//                         console.log('Java Code:', message.code);
//                         return;
//                 }
//             },
//             undefined,
//             context.subscriptions
//         );
//     });

//     context.subscriptions.push(disposable);
// }

// function deactivate() {}

// function getWebviewContent() {
//     return `<!DOCTYPE html>
// <html lang="en">
// <head>
//     <meta charset="UTF-8">
//     <meta name="viewport" content="width=device-width, initial-scale=1.0">
//     <title>Java Code Editor</title>
//     <style>
//         body {
//             font-family: Arial, sans-serif;
//             margin: 0;
//             padding: 0;
//             display: flex;
//             flex-direction: column;
//             height: 100vh;
//         }
//         textarea {
//             flex: 1;
//             width: 100%;
//             font-size: 14px;
//             font-family: monospace;
//             padding: 10px;
//             box-sizing: border-box;
//         }
//         button {
//             padding: 10px;
//             margin: 5px;
//             cursor: pointer;
//         }
//     </style>
// </head>
// <body>
//     <textarea id="javaCode" placeholder="Write your Java code here..."></textarea>
//     <button id="submit">Submit</button>
//     <button id="clear">Clear</button>
//     <script>
//         const vscode = acquireVsCodeApi();

//         document.getElementById('submit').addEventListener('click', () => {
//             const javaCode = document.getElementById('javaCode').value;
//             vscode.postMessage({ command: 'submit', code: javaCode });
//         });

//         document.getElementById('clear').addEventListener('click', () => {
//             document.getElementById('javaCode').value = '';
//         });
//     </script>
// </body>
// </html>`;
// }

// module.exports = {
//     activate,
//     deactivate
// };

const vscode = require('vscode');

/**
 * @param {vscode.ExtensionContext} context
 */
function activate(context) {
    console.log('Extension "popup-test" is now active!');

    let disposable = vscode.commands.registerCommand('popup-test.helloWorld', async function () {
        // // Create a new untitled `.java` file
        // const document = await vscode.workspace.openTextDocument({
        //     content: '// Write your Java code here\n',
        //     language: 'java'
        // });

        // // Show the document in the editor
        // await vscode.window.showTextDocument(document);

		// // Create a new untitled `.java` file with the default template
        // const document = await vscode.workspace.openTextDocument({
        //     content: javaClassTemplate,
        //     language: 'java'

		// Define the default Java class template
		const javaClassTemplate = `
		public class Main {
			public static void main(String[] args) {
				System.out.println("Hello, World!");
			}
		}
        `.trim();

        // Create a new untitled `.java` file with the default template
        const document = await vscode.workspace.openTextDocument({
            content: javaClassTemplate,
            language: 'java'
        });

        // Show the document in the editor
        await vscode.window.showTextDocument(document);
    });

    vscode.workspace.onDidSaveTextDocument((document) => {
        if (document.languageId === 'java') {
            // Log the content of the saved file to the Debug Console
            console.log(`Saved Java file content:\n${document.getText()}`);
            vscode.debug.activeDebugConsole.appendLine(`Saved Java file content:\n${document.getText()}`);
        }
    });

    context.subscriptions.push(disposable);
}

function deactivate() {}

module.exports = {
    activate,
    deactivate
};
