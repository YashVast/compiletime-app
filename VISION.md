# CompileTime — Product Vision & Ideas

> Last updated: April 17, 2026
> Status: Ideas backlog — not yet scheduled

---

## Core Idea
Transform dead time (builds, installs, waits) into micro-learning moments.
End goal: an always-engaging tech companion that feels like Instagram for developers
— infinite scroll of useful, fun, personalized content. User never feels bored.

---

## Ideas Backlog

### 1. Language Learning (Duolingo-style)
- Quiz questions in the style of Duolingo for programming languages
- Spaced repetition — show weak areas more often
- Streaks, levels, daily goals
- Could expand beyond coding: spoken languages too

### 2. Email Summarizer
- Connect to Gmail / Outlook
- Important emails summarized during build time
- "You have 3 emails worth reading — here's the gist"
- Keeps user informed without context-switching to inbox

### 3. Specialized Courses (Team / Enterprise)
- Organizations can create custom question banks
- Questions tied to internal tools, internal processes, company tech stack
- Individual team tracks (frontend team gets React questions, infra team gets K8s)
- Enterprise tier: admin dashboard for managers

### 4. Manager Blog → QnA Pipeline
- Manager drops a blog link or article URL
- AI reads it and auto-generates QnA for the team
- Team gets quizzed on it during their build time
- Makes knowledge sharing frictionless

### 5. Multiplayer / Team Competition
- Employees compete for XP within a team
- Real-time or weekly leaderboard
- End-of-month reward system (badges, recognition, maybe gift cards)
- Healthy competition drives engagement

### 6. Morning Joke / Real-Time Fun
- Daily joke or meme tied to real-world tech events
- Lightweight, fun opener — makes people WANT to open the app
- Something like "Google went down again, here's a joke about it"
- Personalized by stack (JS devs get different jokes than DevOps engineers)

### 7. Book Pages (Micro-Reading)
- A page or two from a tech book surfaces during idle time
- Could tie into courses (manager assigns a book, team reads it 2 pages at a time)
- Progress tracking per book

### 8. Infinite Scroll Feed (Long-term North Star)
- Instagram-style feed but for tech + fun
- Mix of: quizzes, jokes, book pages, news, team updates, course content
- AI-personalized based on what the user engages with
- Could become the primary reason people open the app beyond build detection

---

## AI Pipeline (Blog → QnA + Feed Personalization)

This is a backend service that becomes a core differentiator:

```
Manager drops a URL
    → Backend fetches the article content
    → Sends to Claude API:
       "Generate 5 MCQ questions from this article,
        difficulty: medium, category: infer from content"
    → Claude returns structured QnA
    → Questions stored in Postgres under that org's question bank
    → Questions start appearing in team members' overlays during builds
```

Same pipeline powers:
- Book pages → auto-generated comprehension questions
- Internal docs → onboarding quizzes for new hires
- Morning joke → "summarize today's top tech news, make it funny"
- Feed personalization → "user gets JS questions wrong often, surface more of those"

**Why Claude API specifically:**
- Structured output (JSON questions with correctIndex, explanation, xp)
- Can be prompted with company-specific context
- Handles varying article quality gracefully
- Can adjust difficulty based on user's past performance

This makes CompileTime's content infinite and zero-maintenance for managers.
No one needs to write questions manually.

---

## What This Means for the Product

CompileTime is evolving from:
> "A quiz popup during builds"

To:
> "A developer engagement platform that lives on your desktop and makes
>  every idle moment valuable — learning, fun, connection, competition"

The build detection is the entry point. The feed, courses, and social layer
are what make people stay.
