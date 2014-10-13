from flask import Flask, render_template


app = Flask(__name__,
            template_folder="../../web/registration/",
            static_folder="../../web/registration/",
            static_url_path="")


@app.route("/")
def main():
    return render_template("index.html")


if __name__ == "__main__":
    app.run(debug=True)
