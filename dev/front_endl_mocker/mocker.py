import json
from flask import Flask, render_template, request


app = Flask(__name__,
            template_folder="../../web/registration/",
            static_folder="../../web/registration/",
            static_url_path="")


@app.route("/api/reg/validateKey", methods=['POST'])
def validate_key():
    # test data
    if request.json["email"] != "test@test.com" or request.json["key"] != "pouet":
        return json.dumps({
            "errorKey": "INVALID_UNLOCK_KEY",
            "message":"The unlock key used is invalid or has been used before",
            "url": None
        })

    return json.dumps({
        "user":
        {
            "id": 0,
            "email": "test@test.com",
            "name": None,
            "lastName": None,
            "street": None,
            "postalCode": None,
            "municipality": None,
            "birthPlace": None,
            "birthDate": None,
            "enabled": False,
            "certId": None,
            "country": None
        },
        "timeInitiated": 1413231972887,
        "ipv4Id": 0,
        "ipv6Id": 0,
        "unlockKey":
        {
            "key": "pouet",
            "email": "test@test.com",
            "usedAt": None
        },
        "completed": None,
        "id": "7ed0cea2-1eef-43a9-b7db-b372aa67700f"
    })



@app.route("/")
def main():
    return render_template("index.html")


if __name__ == "__main__":
    app.run(debug=True)
