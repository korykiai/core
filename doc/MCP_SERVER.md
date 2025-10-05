# MCP-Server

## gitlab project

Sourcecode for MCP-Server is located in here: [demo](https://gitlab.com/korykiai/demo "").

## Claude Desktop

koryki.ai has a STDIO MCP-Server ready to use by Claude-Desktop.

To configure Claude-Desktop you need to create or modify your `claud_desktop_config.json` file.

See: 
[Model Context Protocol: https://modelcontextprotocol.io/docs/develop/connect-local-servers](https://modelcontextprotocol.io/docs/develop/connect-local-servers "")

You need to adjust two paths in config file and release version of executable jar file.

For macOS:

`~/Library/Application Support/Claude/claude_desktop_config.json`


    {
        "mcpServers": {
            "korykiai": {
                "command": "/path_to_java/java",
                "args": [
                    "-jar",
                    "/path_to_executable_jar/starter-0.1.0.jar"
                ]
            }
        }
    }

or use file: [claude_desktop_config.json ](./macos/claude_desktop_config.json "")


For Window:

`C:\Users\[user]\AppData\Roaming\Claude\claude_desktop_config.json`

    {
        "mcpServers": {
            "korykiai": {
                "command": "C:\path_to_java\java",
                "args": [
                    "-jar",
                    "C:\path_to_executable_jar\starter-0.1.0.jar"
                ]
            }
        }
    }

or use file: [claude_desktop_config.json ](./windows/claude_desktop_config.json "")


Unix is not yet supported by Claude Desktop, but is announced to come soon.


The koryki.ai MCP-Server has required properties set for default usage. If required adjust properties by adding
them in config file:


    {
        "mcpServers": {
            "korykiai": {
                "command": "/path_to_java/java",
                "args": [
                    -Dlogging.level.root=INFO
                    -Dlogging.level.ai.koryki.starter=DEBUG
                    -Dlogging.file.name=./logs/mcp-korykiai.log
                    "-jar",
                    "/path_to_executable_jar/starter-0.1.0.jar"
                ]
            }
        }
    }
