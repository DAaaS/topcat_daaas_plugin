Hopefully topcat won't break before we stop using it, but if it does here's how to get the FAQ and calendar pages working:
Copy calendar and FAQ html files into topcat's pages directory, topcat should be under the applications directory where this daaas plugin is also installed.
Once the pages are copied, you'll need to modify topcat.json to display them in the header bar of topcat
make sure the pages section in that .json file looks like this
```json
       "pages" : [
            {
                "name" : "contact",
                "addToNavBar": {
                    "label" : "Contact",
                    "align" : "left"
                }

            },
            {
                "name" : "FAQ",
                "addToNavBar": {
                    "label" : "FAQ",
                    "align" : "left"
                }

            },
            {
                "name" : "calendar",
                "addToNavBar": {
                    "label" : "Maintenance Calendar",
                    "align" : "left"
                }

            },
            {
                "name" : "cookie-policy"
            }

        ]
```
