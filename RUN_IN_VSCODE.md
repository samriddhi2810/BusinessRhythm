# Run Business rhythm in VS Code

These steps use Windows and the PowerShell terminal in VS Code. Run the Java backend and React frontend in separate terminals, with MySQL running in the background.

**Local login:** username `admin`, password `change-me-local`.

## 1. Install the tools once

| Tool | Version for this project | Purpose |
|---|---|---|
| [VS Code](https://code.visualstudio.com/) | Current installed version | Editor and terminals |
| [JDK](https://adoptium.net/temurin/releases/?version=21) | 21 | Compile and run the Java backend |
| [Apache Maven](https://maven.apache.org/install.html) | 3.9+ | Install Java dependencies and run Spring Boot |
| [Node.js](https://nodejs.org/en/download) | 22.12+ or 24, including npm | Install and run the React frontend |
| [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) | 8.0.16+ or 8.4 | Database; this app was also run against 8.0.40 |
| [Git](https://git-scm.com/downloads) | Installed Git | Clone and update the repository |

Install MySQL **Server**, not only MySQL Workbench. During installation, remember the MySQL root password you choose and let the MySQL Windows service start. Workbench is an optional graphical client.

For Maven, extract its binary ZIP into a permanent folder and add that folder's `bin` directory to your Windows user `Path`. Set `JAVA_HOME` to your JDK folder if necessary. Restart VS Code after changing environment variables. The [official Maven installation guide](https://maven.apache.org/install.html) explains the setup.

In VS Code, choose **Terminal > New Terminal**. Select **PowerShell** from the terminal menu and check:

```powershell
java -version
javac -version
mvn -version
node --version
npm.cmd --version
git --version
```

Use `npm.cmd` in Windows PowerShell to avoid script-activation policy issues. Both Java commands should report version 21. If a command is not found, finish installing that tool or adding its `bin` folder to `Path`, then open a new terminal.

For Java editing and debugging, install **Extension Pack for Java** and **Spring Boot Extension Pack** from the VS Code Extensions panel (`Ctrl+Shift+X`). These are the extensions listed in [VS Code's Spring Boot guide](https://code.visualstudio.com/docs/java/java-spring-boot). VS Code already supports JavaScript and React editing; no Python extension is needed to run this app.

## 2. Open the project folder

If you have not downloaded it, run this from the folder where you keep your projects:

```powershell
git clone https://github.com/samriddhi2810/business-rhythm.git
cd business-rhythm
code .
```

If `code` is not recognized, open VS Code and choose **File > Open Folder**, then select `business-rhythm`. If you extracted the source ZIP, select the extracted folder that contains `backend`, `frontend`, and `README.md`.

If you already have the project, open its existing folder instead of cloning again. Its local folder can still be named `stockflow`; renaming a GitHub repository does not rename an existing folder. To update an existing clone:

```powershell
git remote set-url origin https://github.com/samriddhi2810/business-rhythm.git
git pull --ff-only
```

Keep your own uncommitted edits before pulling. Open **Terminal > New Terminal**; it should start in the project root.

## 3. Create the MySQL database once

Open MySQL Workbench, connect to your local MySQL Server with your root credentials, and run the SQL below in a query tab. Alternatively, open a VS Code terminal and run `mysql -u root -p`, then enter your MySQL root password when prompted and paste the same SQL.

```sql
CREATE DATABASE IF NOT EXISTS stockflow;
CREATE USER IF NOT EXISTS 'stockflow'@'localhost' IDENTIFIED BY 'stockflow_local';
GRANT ALL PRIVILEGES ON stockflow.* TO 'stockflow'@'localhost';
```

These are local development credentials. If that MySQL user already exists, `CREATE USER IF NOT EXISTS` keeps its existing password. In that case, use that user's known password in `DB_PASSWORD` in the next step, or create a fresh user of your choice and set `DB_USER` to match.

If using the MySQL terminal, enter `exit` to leave it. Spring Boot creates the application tables on its first successful database connection; you do not need to run `backend/src/main/resources/schema.sql` manually.

The database and Java package names still use `stockflow` internally. The application name shown to users is **Business rhythm**.

## 4. Start the backend in terminal 1

From the project root:

```powershell
cd backend
$env:DB_URL = 'jdbc:mysql://localhost:3306/stockflow'
$env:DB_USER = 'stockflow'
$env:DB_PASSWORD = 'stockflow_local'
$env:APP_USER = 'admin'
$env:APP_PASSWORD = 'change-me-local'
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.address=127.0.0.1"
```

Change the database username/password/port above if your MySQL installation uses different values. The first run downloads Java dependencies, so allow it to finish. Wait for a message containing `Started Application` and `Tomcat started on port 8080`. Leave this terminal running.

These `$env:` settings apply to this terminal and the backend it launches. They are not written to Git. A root `.env` file is read by Docker Compose, but it is **not automatically loaded** by this direct Maven command.

## 5. Start the frontend in terminal 2

Choose **Terminal > New Terminal** again. In a new terminal at the project root, run:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev -- --port 5173 --strictPort
```

`npm ci` installs the exact frontend dependencies from the lockfile. The frontend runs at `http://127.0.0.1:5173` and forwards `/api` requests to the backend on port 8080. Leave this terminal running too.

## 6. Open the app and log in

Open [Business rhythm](http://127.0.0.1:5173) in your browser.

| Field | Local default |
|---|---|
| Username | `admin` |
| Password | `change-me-local` |

If you changed `APP_USER` or `APP_PASSWORD` in terminal 1, use your new values. These app login credentials are separate from your MySQL root credentials.

A fresh database starts empty. Add a product in **Products**, set stock and a minimum, create an order in **Orders**, and confirm it to deduct stock. Pack and complete an order to include it in **Reports**.

## 7. Stop and start it next time

Press `Ctrl+C` in both app terminals to stop the backend and frontend. Leave the MySQL service running if you use it for other projects. Database records remain stored in MySQL.

Next time, start MySQL if needed, repeat step 4 in terminal 1 and run `npm.cmd run dev -- --port 5173 --strictPort` from `frontend` in terminal 2. Repeat `npm.cmd ci` after pulling changes that update the dependency lockfile.

## Optional: Python virtual environment and requirements.txt

Python is not part of this application's runtime. The requested `requirements.txt` intentionally contains comments only. `pip` cannot install Java, Maven, Node.js, React's npm packages, or MySQL Server.

If you want an optional Python environment for your own tooling, run these commands from the project root with Python installed:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

The second command succeeds without installing any Python packages. You do not need to activate the environment. You must still follow steps 1–6 to run Business rhythm. `.venv` is excluded from Git.

## Common problems

| Problem | What to do |
|---|---|
| `mvn` not recognized | Add Maven's `bin` folder to your user `Path`, restart VS Code, and check `mvn -version`. |
| `release version 21 not supported` | Use JDK 21 and check the Java version printed by `mvn -version`. |
| `mysql` not recognized | Run the setup SQL using Workbench, or add MySQL Server's `bin` directory to `Path`. |
| Backend says `Access denied for user` | Check `DB_USER` and `DB_PASSWORD` against the MySQL account you created. |
| Database connection refused | Start MySQL Server and check the host/port in `DB_URL`. |
| Port 8080 or 5173 is already in use | Reuse your existing demo or stop its app process first; avoid starting a second copy on the same ports. |
| Browser cannot reach the backend | Check that both terminals are still running and the backend is on port 8080. |
| Login rejected | Use the values of `APP_USER` and `APP_PASSWORD` that were present when the backend started. |
| Download errors on the first run | Maven and npm require internet access to download dependencies. |

## Checks you can run

In a terminal opened at the project root, run backend tests with:

```powershell
cd backend
mvn test
```

In another terminal opened at the project root, check the frontend production build with:

```powershell
cd frontend
npm.cmd run build
```

The backend tests use an isolated H2 test database, so they do not require your live MySQL database.

## Docker alternative

If you prefer Docker Desktop, follow the Docker section of `README.md` instead of the manual database/backend/frontend steps. Do not start both workflows on the same ports.

## Official references

- [VS Code: Spring Boot](https://code.visualstudio.com/docs/java/java-spring-boot)
- [VS Code: React and Node.js](https://code.visualstudio.com/docs/nodejs/reactjs-tutorial)
- [Maven installation](https://maven.apache.org/install.html)
