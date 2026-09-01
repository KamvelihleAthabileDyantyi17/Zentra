# Zentra Project: Shannon's Food Truck Digital Platform

## 📌 Project Overview
This project involves developing a digital ordering and management platform for Shannon’s fast-food truck business[cite: 4]. The business specializes in local comfort food like amagwinya, beverages, and Kotas, typically serving customers with a quick turnaround[cite: 4]. 

As the business scales its operations from two to three active trucks, this digital solution aims to accelerate growth, enhance brand exposure, and cultivate a stronger community presence[cite: 4].

## 🎯 Objectives
* **Integrated Portals:** Develop a seamless Customer Portal (Menu, Cart, Checkout, Order Tracking) and a robust Admin Dashboard (Menu Management, Live Inventory)[cite: 4].
* **Process Automation:** Enable customers to place orders online while automatically deducting real-time inventory for the kitchen staff[cite: 4].
* **Scalable Infrastructure:** Transition from local testing environments to live, scalable cloud tiers utilizing Supabase or Firebase to handle concurrent multi-truck operations[cite: 4].

## 👥 The Team (Execution Phase Roles)
Following the planning phase, team roles have evolved into a specialized Continuous Integration/Continuous Deployment (CI/CD) pipeline:
* **Kamv’elihle Dyantyi (Team Lead):** Manages the Azure DevOps sprint planning, oversees the GitHub repository structure (`KamvelihleAthabileDyantyi17/Zentra`), writes DevOps lifecycle documentation, and enforces Pull Request (PR) code reviews[cite: 4].
* **Katleho Khutsoane (UI/UX & Design):** Designs the complete application sitemap and UI wireframes, and manages the formatting and compilation of final client deliverables[cite: 4].
* **Neziswa Bikitsha (Requirement Analysis):** Translates operational requirements into the functional Client Needs documentation[cite: 4].
* **Amandla Sekeleni (Database & Development):** Translates approved wireframes into functional HTML code and structures the backend ERD database connections[cite: 4].

## 🛠️ Tech Stack & DevOps Pipeline
* **Project Management:** Azure DevOps Boards (Sprints & Task Tracking)
* **Version Control:** Git & GitHub (Feature Branching Strategy)
* **Frontend Design & Code:** Figma, HTML, CSS[cite: 4]
* **Backend & Mobile Logic:** Kotlin, SQL[cite: 4]
* **Testing Environment:** Physical Android devices to bypass emulator hardware bottlenecks[cite: 4]

## 📅 Current Sprint Roadmap (Task 1 & 2)
The project has shifted from isolated planning into an interconnected, agile development workflow:
1. **Sept 1 – Sept 4 (Task 1 Drop):** Finalize the overarching Project Plan, Customer/Admin Site Map, initial UI wireframes, and DevOps deployment pipeline documentation.
2. **Sept 5 – Sept 19 (Sprint 1):** Kat hands off finalized UI assets; Ama codes the HTML boilerplate and creates PRs. Kamva scaffolds the live database connections.
3. **Sept 20 – Oct 4 (Sprint 2):** Connect frontend views to backend MVC controllers and API endpoints (Authentication, Order Processing).
4. **Oct 19 (Task 2 Drop):** Code freeze. Final end-to-end testing of real-time WebSocket tracking and inventory sync prior to final submission.

## ⚠️ Current Development Status & Rules
* **Strict PR Workflow:** No direct commits to the `main` branch are permitted. All code updates by developers must be pushed via feature branches (e.g., `feature/admin-inventory`) and reviewed by the Team Lead against the approved UI designs.
* **Hardware Mitigation:** Due to severe resource-heavy performance bottlenecks with Android Virtual Devices (emulators) within Android Studio, all mobile deployment testing is actively routed through physical Android smartphones[cite: 4].
