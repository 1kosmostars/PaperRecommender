# -*- coding: utf-8 -*-
from docx import Document
from docx.shared import Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
import os

doc = Document()
def set_rtl(p):
    pPr=p._p.get_or_add_pPr()
    b=OxmlElement('w:bidi');b.set(qn('w:val'),'1');pPr.append(b)
    j=OxmlElement('w:jc');j.set(qn('w:val'),'right');pPr.append(j)
def rr(run):
    rPr=run._r.get_or_add_rPr();rPr.append(OxmlElement('w:rtl'));rPr.append(OxmlElement('w:cs'))
def H(lv,txt):
    p=doc.add_heading(txt,level=lv);set_rtl(p)
    for r in p.runs:rr(r);r.font.name='David'
    return p
def P(txt,bold=False,sz=12,color=None,sa=6,sb=0):
    p=doc.add_paragraph();set_rtl(p)
    p.paragraph_format.space_after=Pt(sa);p.paragraph_format.space_before=Pt(sb)
    r=p.add_run(txt);r.font.name='David';r.font.size=Pt(sz);r.bold=bold;rr(r)
    if color:r.font.color.rgb=RGBColor(*color)
    return p
def B(txt):
    p=doc.add_paragraph(style='List Bullet');set_rtl(p)
    r=p.add_run(txt);r.font.name='David';r.font.size=Pt(12);rr(r)
def N(txt):
    p=doc.add_paragraph(style='List Number');set_rtl(p)
    r=p.add_run(txt);r.font.name='David';r.font.size=Pt(12);rr(r)
def C(txt):
    p=doc.add_paragraph();p.paragraph_format.space_after=Pt(1)
    r=p.add_run(txt);r.font.name='Courier New';r.font.size=Pt(9)
    return p
def T(headers,rows):
    t=doc.add_table(rows=1+len(rows),cols=len(headers));t.style='Light Grid Accent 1'
    for i,hd in enumerate(headers):
        c=t.rows[0].cells[i];c.text=hd
        for p in c.paragraphs:set_rtl(p)
        for p in c.paragraphs:
            for r in p.runs:r.bold=True;r.font.name='David';r.font.size=Pt(10);rr(r)
    for ri,row in enumerate(rows):
        for ci,val in enumerate(row):
            c=t.rows[ri+1].cells[ci];c.text=val
            for p in c.paragraphs:set_rtl(p)
            for p in c.paragraphs:
                for r in p.runs:r.font.name='David';r.font.size=Pt(10);rr(r)
    return t
def PB():doc.add_page_break()
def PLACEHOLDER(txt):
    P('['+txt+']',sz=11,color=(180,0,0))
def code_file(path):
    full=os.path.join(r'C:\Users\ikomostars\IdeaProjects\PaperRecommender',path)
    try:
        with open(full,'r',encoding='utf-8') as f:return f.read()
    except:return '// FILE NOT FOUND: '+path

style=doc.styles['Normal'];style.font.name='David';style.font.size=Pt(12)
section=doc.sections[0]
hp=section.header.paragraphs[0]
hp.text="PaperRecommender | מערכת המלצת מאמרים אקדמיים"
for r in hp.runs:r.font.name='David';r.font.size=Pt(9)
fp=section.footer.paragraphs[0];fp.alignment=WD_ALIGN_PARAGRAPH.CENTER
r=fp.add_run();fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'PAGE');r._r.append(fld)

########################################
# COVER
########################################
doc.add_paragraph();doc.add_paragraph()
P('מערכת המלצת מאמרים אקדמיים',bold=True,sz=28,color=(31,78,121),sb=60)
P('Paper Recommender System',sz=16,color=(68,114,196))
P('ספר פרויקט – י״ג הנדסת תוכנה',bold=True,sz=14,sb=20)
doc.add_paragraph()
P('שם מכללה: ______________________',sz=12)
P('סמל מוסד: ______________________',sz=12)
P('שם הסטודנט: ______________________',sz=12)
P('ת.ז. הסטודנט: ______________________',sz=12)
P('שם המנחה: ______________________',sz=12)
P('תאריך הגשה: מאי 2026',sz=12)
PB()

########################################
# TOC
########################################
H(1,'תוכן עניינים')
PLACEHOLDER('יש לייצר תוכן עניינים אוטומטי ב-Word: References > Table of Contents. יש להשתמש בכותרת 1, כותרת 2, כותרת 3')
PB()

########################################
# 1. ABSTRACT
########################################
H(1,'1. תקציר – תיאור הפרויקט ותהליך העבודה')
P('מערכת ההמלצות פותחה כדי להציע פתרון לבעיה המרכזית של חוקרים וסטודנטים — כיצד למצוא מאמרים אקדמיים רלוונטיים מתוך מיליוני מאמרים שמתפרסמים מדי שנה. המערכת ממליצה על מאמרים המתאימים לתחום העניין של המשתמש, על בסיס היסטוריית האינטראקציות שלו, תוכן המאמרים, ומבנה גרף הציטוטים — וזאת ללא שימוש בספריות למידת מכונה מוכנות.')
P('היישום נכתב ב-Java 17 בסביבת IntelliJ IDEA, עם Spring Boot 3.2.5, ומממש שישה אלגוריתמי-ליבה — HNSW, BM25, BPR, PageRank, Reciprocal Rank Fusion ו-MMR — כולם ללא ספריות מוכנות. האלגוריתמים הביצועיים (HNSW, BPR, PageRank) ממומשים בשפת C ומחוברים דרך JNI.')
P('המערכת פועלת במבנה MVC: שכבת web (Controllers ו-DTOs) משמשת כ-View/Controller, שכבת service מכילה את הלוגיקה העסקית (Model), ושכבת db מנהלת את הגישה למסד הנתונים PostgreSQL.')
doc.add_paragraph()
P('תהליך העבודה (בסדר כרונולוגי):',bold=True)
N('הגדרת טיפוסים בסיסיים — מחלקות Paper, User, Interaction עם שדות מזהים, מטא-נתונים ו-embedding')
N('הקמת מסד נתונים — PostgreSQL (Neon Cloud) עם מיגרציה V1__init.sql דרך Flyway')
N('מימוש לקוחות API — SemanticScholarClient (מקור ראשי: מאמרים + embeddings + ציטוטים), ArxivClient, OpenAlexClient')
N('מימוש אלגוריתם HNSW בשפת C — מבנה גרף רב-שכבתי לחיפוש k-NN מקורב')
N('מימוש BM25 ב-Java — אינדקס הפוך עם IDF ונורמליזציה לאורך מסמך')
N('מימוש BPR בשפת C — סינון שיתופי עם SGD ודגימה שלילית')
N('מימוש PageRank בשפת C — דירוג צמתים בגרף ציטוטים איטרטיבי')
N('מימוש Fusion (RRF) ו-MMR ב-Java — מיזוג תוצאות וגיוון')
N('בניית צינור ההמלצה — RecommendationService המשלב את כל 4 האותות')
N('בניית ממשק משתמש — index.html (חיפוש + כרטיסי מאמרים) ו-admin.html (קונסול ניהול)')
doc.add_paragraph()
P('תוצאות מרכזיות:',bold=True)
B('חיפוש HNSW ב-O(log N) במקום O(N) — כ-50 מאמרים בפחות מ-10ms')
B('BPR מאומן על אינטראקציות — 100 epochs × 64 factors')
B('PageRank על גרף ציטוטים — 20 איטרציות, d=0.85')
B('מיזוג RRF ממזג 4 אותות ללא נורמליזציה + MMR מגוון עם λ=0.7')
PB()

########################################
# 2. PROJECT PROPOSAL
########################################
H(1,'2. הצעת הפרויקט שאושרה')
PLACEHOLDER('יש לצרף כאן את הצעת הפרויקט המקורית שאושרה על ידי משרד החינוך, כולל חתימות הסטודנט, המנחה ורכז המגמה')
PB()

########################################
# 3. DECLARATION
########################################
H(1,'3. תצהיר')
P('אני החתום/ה מטה מצהיר/ה בזאת כי עבודה זו נעשתה על ידי בלבד. לא העתקתי חלקים מעבודה אחרת ולא השתמשתי בספריות מוכנות המממשות את האלגוריתמים המרכזיים במערכת (HNSW, BM25, BPR, PageRank, RRF, MMR). כל האלגוריתמים נכתבו על ידי מאפס.')
doc.add_paragraph();doc.add_paragraph()
P('שם: ______________________          ת.ז.: ______________________          חתימה: ______________________')
P('תאריך: ______________________')
PB()

########################################
# 4. CONCEPTS
########################################
H(1,'4. מושגים')
P('להלן מושגים ספציפיים שנדרשתי להגדיר לצורך פתרון בעיית הפרויקט. אין כאן מושגים כלליים במדעי המחשב, אלא מושגים הנוגעים ישירות למערכת ההמלצות שפיתחתי.')
doc.add_paragraph()
concepts = [
    ('Embedding (וקטור הטמעה)','ייצוג מספרי של טקסט מאמר כמערך של 768 מספרים עשרוניים (float) במרחב וקטורי רב-ממדי. מאמרים דומים מבחינה סמנטית יהיו קרובים במרחב הזה.','בפרויקט: כל מאמר שנאכל מ-Semantic Scholar מקבל וקטור SPECTER2 באורך 768 ממדים, שנשמר כ-bytea במסד הנתונים (little-endian float32). הוקטור משמש לחיפוש k-NN ב-HNSW ולחישוב פרופיל המשתמש.'),
    ('HNSW (Hierarchical Navigable Small World)','מבנה נתונים גרפי רב-שכבתי המאפשר חיפוש שכנים קרובים (ANN) בזמן O(log N) במקום O(N). בשכבות העליונות יש "קפיצות ארוכות" (כמו skip list), ובשכבה 0 יש חיפוש beam מפורט.','בפרויקט: ממומש מאפס בשפת C (hnsw.c, 377 שורות). משמש כאות ראשון בצינור ההמלצה — מוצא מאמרים דומים לפרופיל המשתמש. פרמטרים: M=16, ef_construction=200, ef_search=50.'),
    ('BM25 (Okapi Best Match 25)','אלגוריתם לדירוג מסמכים לפי שאילתת טקסט. משלב TF (תדירות מונח) עם IDF (חשיבות מונח) ונורמליזציה לאורך המסמך. עדיף על TF-IDF בכך שה-TF רווי (saturated) ולא גדל ליניארית.','בפרויקט: ממומש ב-Java (Bm25Index.java, 202 שורות). משמש כאות שלישי — כשהמשתמש מקליד שאילתת חיפוש חופשית. k1=1.2, b=0.75.'),
    ('BPR (Bayesian Personalized Ranking)','אלגוריתם לסינון שיתופי (collaborative filtering) מ-implicit feedback. לומד לדרג זוגות פריטים: "מאמר שהמשתמש אינטראקט איתו עדיף על מאמר אקראי". משתמש ב-SGD עם דגימה שלילית.','בפרויקט: ממומש בשפת C (bpr.c, 166 שורות). מטריצת פירוק עם 64 factors, 100 epochs, lr=0.01, λ=0.001. משמש כאות שני בצינור.'),
    ('PageRank','אלגוריתם דירוג צמתים בגרף מכוון (Brin & Page, 1998). כל צומת מעביר חלק מהניקוד שלו לשכניו. צומת שמקבל הרבה קישורים מצמתים חשובים — ייחשב חשוב יותר.','בפרויקט: ממומש בשפת C (pagerank.c, 62 שורות). מורץ על גרף הציטוטים: אם מאמר A מצטט מאמר B, יש קשת A→B. 20 איטרציות, d=0.85. משמש כאות רביעי.'),
    ('RRF (Reciprocal Rank Fusion)','שיטת מיזוג של מספר רשימות מדורגות לרשימה אחת (Cormack et al., 2009). הנוסחה: score(d) = Σ 1/(k + rank(d)), כאשר k=60. יתרון: לא דורש נורמליזציה של ציונים מאלגוריתמים שונים.','בפרויקט: ממומש ב-Java (FusionReranker.java, 46 שורות). ממזג 4 רשימות: HNSW, BPR, BM25, PageRank.'),
    ('MMR (Maximal Marginal Relevance)','אלגוריתם לגיוון תוצאות (Carbonell & Goldstein, 1998). בכל צעד בוחר את הפריט עם הציון הגבוה ביותר של: λ·rel(d) − (1−λ)·max_sim(d, selected). מפחית כפילויות.','בפרויקט: ממומש ב-Java (MmrReranker.java, 114 שורות). λ=0.7 — עדיפות לרלוונטיות עם מעט גיוון. מחשב cosine similarity בין כל זוג.'),
    ('Cosine Similarity','מדד דמיון בין שני וקטורים: cos(θ) = dot(A,B) / (||A||·||B||). טווח [−1, 1]. ערך 1 = זהים, 0 = ניצבים, −1 = הפוכים.','בפרויקט: משמש ב-HNSW כמדד מרחק (cosine distance = 1 − cosine similarity), וב-MMR לחישוב דמיון בין מאמרים שנבחרו.'),
    ('Implicit Feedback (משוב מרומז)','מידע על העדפות המשתמש שמתקבל לא מדירוג מפורש (כוכבים) אלא מהתנהגות: צפייה, לחיצה, שמירה, הורדה. כל פעולה מקבלת משקל שונה.','בפרויקט: VIEW=0.2, CLICK=0.5, SAVE=1.0, DOWNLOAD=1.5, CITE=3.0. משמש לאימון BPR ולחישוב פרופיל המשתמש עם דעיכה אקספוננציאלית (חצי-חיים 30 ימים).'),
    ('JNI (Java Native Interface)','ממשק סטנדרטי של Java המאפשר קריאה לפונקציות בשפת C. הקוד ב-C מקומפל ל-DLL, ו-Java טוען אותו בזמן ריצה.','בפרויקט: שלושה קבצי JNI: HnswNative.java, BprNative.java, PageRankNative.java. NativeLoader טוען את ה-DLLs מ-classpath (/native/*.dll).'),
    ('Inverted Index (אינדקס הפוך)','מבנה נתונים שממפה כל מונח (term) לרשימת המסמכים שמכילים אותו, כולל מספר ההופעות (TF). מאפשר חיפוש יעיל — במקום לסרוק כל מסמך, פונים ישירות לרשימה.','בפרויקט: נבנה בתוך Bm25Index.build(). המפתח הוא String (מונח), הערך הוא List<int[]> — זוגות של [docId, tf].'),
    ('Exponential Decay (דעיכה אקספוננציאלית)','פונקציה מתמטית שנותנת משקל גבוה לאירועים אחרונים ומשקל נמוך לישנים: w = e^(−λ·t). חצי-חיים = ln(2)/λ.','בפרויקט: פרופיל המשתמש מחושב כממוצע משוקלל של embeddings עם דעיכה. חצי-חיים = 30 ימים. אינטראקציה מלפני 30 יום מקבלת חצי מהמשקל של אינטראקציה של היום.'),
]
for name,defn,proj in concepts:
    P(name,bold=True,sz=12,sb=6)
    P(defn,sz=12)
    P(proj,sz=11,color=(50,50,130))
PB()

########################################
# 5. TOPIC DESCRIPTION
########################################
H(1,'5. תיאור הנושא')
P('הנושא של הפרויקט הוא מערכת המלצת מאמרים אקדמיים. המטרה היא לפתח מערכת שיודעת להציע לחוקר או לסטודנט מאמרים שרלוונטיים לתחום העניין שלו, על סמך מה שהוא קרא, שמר וחיפש בעבר — וזאת ללא שימוש בספריות מוכנות לעיבוד שפה טבעית או למידת מכונה.')
P('בעולם האקדמי מתפרסמים מעל 2.5 מיליון מאמרים בשנה. חוקר ממוצע לא יכול לקרוא את כולם, ולכן הוא זקוק לכלי שיסנן עבורו את המאמרים הרלוונטיים ביותר. מערכת ההמלצות שפיתחתי פותרת את הבעיה הזו בגישה היברידית — משלבת ארבעה אותות שונים:')
N('אות תוכני (Content-Based): חיפוש מאמרים דומים מבחינה סמנטית באמצעות וקטורי embedding וחיפוש HNSW')
N('אות שיתופי (Collaborative Filtering): למידה מדפוסי ההתנהגות של המשתמש עם BPR')
N('אות טקסטואלי (Text Search): חיפוש טקסט חופשי עם BM25')
N('אות גרפי (Graph-Based): דירוג מאמרים לפי חשיבות בגרף הציטוטים עם PageRank')
P('התוצאות מהארבעה אותות ממוזגות ב-Reciprocal Rank Fusion (RRF) ומגוונות ב-Maximal Marginal Relevance (MMR) להבטחת מגוון.')
P('המערכת כוללת ממשק ווב עם אפשרות חיפוש, צפייה בכרטיסי מאמרים, שמירת מאמרים, והוספה לרשימת הפניות אישית. בנוסף קיים קונסול ניהול לאכילת מאמרים ממקורות חיצוניים (Semantic Scholar, ArXiv), בנייה מחדש של אינדקסים, וצפייה בסטטיסטיקות.')
PB()

########################################
# 6. THEORETICAL BACKGROUND
########################################
H(1,'6. רקע תיאורטי')
P('מערכות המלצה (Recommender Systems) הן מהכלים הנפוצים ביותר באינטרנט. Amazon, Netflix, Spotify ו-Google Scholar כולם משתמשים באלגוריתמי המלצה. בתחום האקדמי, מערכות כמו Semantic Scholar, ResearchGate ו-Google Scholar מציעות המלצות מאמרים, אבל הן משתמשות בשירותי ענן ומודלים סגורים שלא ניתן ללמוד מהם.')
P('בספרות המקצועית קיימות שלוש גישות עיקריות:', bold=True)
doc.add_paragraph()
H(2,'6.1 סינון מבוסס תוכן (Content-Based Filtering)')
P('גישה שמשתמשת בתכונות הפריטים עצמם. הרעיון: אם למשתמש יש פרופיל, אפשר לחפש פריטים שדומים לפרופיל שלו. השיטות הקלאסיות השתמשו ב-TF-IDF ודמיון קוסינוס, אבל בשנים האחרונות הגישה המקובלת היא להשתמש בוקטורי embedding מרשתות נוירונים שעברו אימון מראש (pretrained).')
P('SPECTER2 (Cohan et al., 2020) הוא מודל מבית Semantic Scholar שמייצר וקטורים באורך 768 ממדים למאמרים. הוא אומן על ציטוטים — מאמרים שמצטטים זה את זה מקבלים וקטורים קרובים. אני מקבל את הוקטורים דרך ה-API של Semantic Scholar ולא מריץ את המודל עצמו.')
P('הבעיה: חיפוש k-NN מדויק ב-768 ממדים הוא O(N) — צריך לעבור על כל מאמר. לכן אני משתמש ב-HNSW (Malkov & Yashunin, 2020) שמספק חיפוש מקורב ב-O(log N).')
doc.add_paragraph()
H(2,'6.2 סינון שיתופי (Collaborative Filtering)')
P('גישה שמתבססת על ההנחה: "משתמשים שנהגו בדומה בעבר ינהגו בדומה בעתיד". אינה צריכה לדעת מה תוכן הפריט — מספיק לדעת מי צרך מה.')
P('בעיה: במערכת שלי אין דירוגים מפורשים (explicit ratings). יש רק implicit feedback — צפייה, לחיצה, שמירה. לכן ALS (Alternating Least Squares), שמיועד ל-explicit ratings, אינו מתאים. בחרתי ב-BPR (Rendle et al., 2009) שתוכנן במיוחד ל-implicit feedback — הוא לומד לדרג זוגות: "מאמר שהמשתמש אינטראקט איתו עדיף על מאמר אקראי".')
doc.add_paragraph()
H(2,'6.3 דירוג מבוסס גרף (Graph-Based Ranking)')
P('PageRank (Brin & Page, 1998), שפותח במקור עבור מנוע החיפוש של Google, מדרג צמתים בגרף לפי חשיבות. הרעיון: צומת (מאמר) חשוב אם הרבה צמתים חשובים מצביעים עליו (מצטטים אותו).')
P('בעולם האקדמי, מאמר שמצוטט על ידי מאמרים חשובים אמור להיות משמעותי יותר. PageRank נותן אות נוסף שאינו תלוי בתוכן או בהתנהגות המשתמש — הוא מבוסס אך ורק על מבנה גרף הציטוטים.')
doc.add_paragraph()
H(2,'6.4 חיפוש טקסט (Text Retrieval)')
P('BM25 (Robertson & Zaragoza, 2009) הוא הסטנדרט המקובל לחיפוש טקסט. הוא עדיף על TF-IDF הפשוט בשני אופנים: (1) TF saturation — תדירות המונח לא גדלה ליניארית; (2) נורמליזציה לאורך מסמך — מסמכים ארוכים לא מקבלים יתרון מופרז.')
doc.add_paragraph()
H(2,'6.5 מיזוג וגיוון')
P('כשיש מספר רנקרים (HNSW, BPR, BM25, PageRank), צריך למזג את התוצאות. RRF (Cormack et al., 2009) ממזג לפי דירוג (rank) ולא לפי ציון (score) — כך אין צורך לנרמל ציונים מאלגוריתמים שונים.')
P('MMR (Carbonell & Goldstein, 1998) מבטיח גיוון — מונע מצב שבו כל התוצאות הן מאמרים דומים מדי זה לזה.')
PB()

########################################
# 7. ALGORITHMIC PROBLEM
########################################
H(1,'7. תיאור הבעיה האלגוריתמית')
P('הבעיה המרכזית: בהינתן מאגר של N מאמרים אקדמיים, משתמש u עם היסטוריית אינטראקציות, ושאילתת טקסט אופציונלית q — יש למצוא את K המאמרים הרלוונטיים ביותר למשתמש.')
P('זוהי בעיה של הכרעת מצבים, ואין קריטריון ברור לפתרון, מהסיבות הבאות:', bold=True)
doc.add_paragraph()
N('אין הגדרה אחת של "רלוונטיות" — מאמר יכול להיות רלוונטי כי הוא דומה סמנטית (embedding), כי משתמשים דומים קראו אותו (collaborative), כי הוא חשוב בתחום (PageRank), או כי הוא מכיל מילות חיפוש (BM25). כל אות נותן תוצאות שונות')
N('אין פתרון אופטימלי מובטח — איך לשקלל את 4 האותות? איזה משקל לתת לכל אחד? RRF נותן משקל שווה, אבל זו היוריסטיקה ולא פתרון מוכח')
N('חיפוש מדויק ב-768 ממדים הוא NP-hard (Curse of Dimensionality) — אלגוריתם מדויק דורש O(N), ולכן אנו נאלצים להשתמש בחיפוש מקורב (HNSW) שלא מבטיח למצוא את התוצאה הטובה ביותר')
N('הפרופיל של המשתמש משתנה בזמן — אינטראקציות ישנות רלוונטיות פחות, ולכן נדרשת דעיכה אקספוננציאלית. בחירת חצי-החיים (30 ימים) היא היוריסטית')
N('סינון שיתופי סובל מ-Cold Start — משתמש חדש ללא היסטוריה לא יקבל המלצות BPR, ולכן המערכת חייבת להשלים עם אותות אחרים')
P('הפתרון הוא שילוב היברידי של מספר אותות עם היוריסטיקות מבוססות מחקר. אין פתרון מושלם — ההכרעה נעשית על בסיס מחקר ומדידות ולא בצורה שרירותית.')
PB()

########################################
# 8. ALGORITHM SURVEY
########################################
H(1,'8. סקירת אלגוריתמים בתחום הבעיה')
P('להלן השוואה בין אלגוריתמים שנבדקו, כולל ניתוח SWOT:')
survey = [
    ('אלגוריתם','סיבוכיות','יתרונות','חסרונות','נבחר?'),
    ('HNSW','O(log N)','מדרגי, מקורב, עובד בממדים גבוהים','דורש זיכרון O(N·M), מקורב','כן'),
    ('k-d Tree','O(N) ב-768 ממדים','מדויק בממדים נמוכים','מתפרק לברוט-פורס ב-768 ממדים','לא'),
    ('LSH','O(1) amortized','פשוט ומהיר','דיוק נמוך מ-HNSW, פרמטרים רבים','לא'),
    ('Brute-Force k-NN','O(N)','מדויק 100%','איטי מדי למאגרים גדולים','לא'),
    ('BM25','O(Q·df)','סטנדרט מוכח, יעיל','לא תופס סמנטיקה','כן'),
    ('TF-IDF','O(Q·N)','פשוט','ללא נורמליזציה לאורך, פחות מדויק','לא'),
    ('BPR','O(epochs·n·F)','מתאים ל-implicit feedback','דורש מספיק אינטראקציות','כן'),
    ('ALS','O(iterations·(U+I)·F²)','מקבילי','מיועד ל-explicit ratings','לא'),
    ('SVD++','O(iterations·nnz·F)','משלב implicit','מורכב למימוש','לא'),
    ('PageRank','O(iter·(E+N))','מבוסס מבנה, לא תוכן','לא אישי לבדו','כן'),
    ('HITS','O(iter·E)','מפריד authority/hub','פחות יציב מ-PageRank','לא'),
    ('RRF','O(R·n)','ללא נורמליזציה','משקל שווה לכל רנקר','כן'),
    ('CombMNZ','O(R·n)','מתחשב במספר הופעות','רגיש לסקאלות','לא'),
    ('MMR','O(k²·dim)','מבטיח גיוון','ריבועי ב-k','כן'),
]
T(survey[0],survey[1:])
PB()

########################################
# 9. CHOSEN STRATEGY (3-4 pages)
########################################
H(1,'9. אסטרטגיה נבחרת לפתרון — תיאור ונימוק')
P('האסטרטגיה הנבחרת היא צינור היברידי (Hybrid Recommendation Pipeline) המשלב 4 אותות → RRF → MMR. להלן תיאור מפורט של כל אלגוריתם והנימוק לבחירתו.')
doc.add_paragraph()

H(2,'9.1 HNSW — חיפוש וקטורי (אות 1: Content-Based)')
P('HNSW (Hierarchical Navigable Small World) הוא מבנה נתונים גרפי לחיפוש שכנים קרובים במרחב וקטורי. נפרסם ב-2020 על ידי Malkov ו-Yashunin ב-IEEE TPAMI.')
P('הרעיון המרכזי: בניית גרף רב-שכבתי שבו:', bold=True)
B('השכבות העליונות מכילות "קשרים ארוכים" (long-range links) שמאפשרים קפיצות מהירות — כמו express lanes בכביש מהיר')
B('השכבה התחתונה (layer 0) מכילה קשרים קצרים שמאפשרים חיפוש מדויק')
B('כל נוד מופיע בשכבה 0 ובמספר שכבות עליונות — לפי התפלגות אקספוננציאלית: level = ⌊−ln(uniform) · mL⌋')
P('חיפוש (hnsw_search):', bold=True)
N('ירידה חמדנית (greedy descent) מהשכבה העליונה: בכל שכבה מוצאים את הנוד הקרוב ביותר ויורדים')
N('בשכבה 0: חיפוש beam עם ef=50 — עוברים על שכנים, שומרים max-heap של ef המועמדים הטובים ביותר')
N('מחזירים k הקרובים ביותר')
P('הוספת נוד (hnsw_add):', bold=True)
N('דגימת רמה: level = ⌊−ln(rand()) · 1/ln(M)⌋')
N('ירידה חמדנית לרמת הנוד')
N('חיפוש ef_construction שכנים בכל שכבה')
N('קשתות דו-כיווניות + pruning אם יותר מ-M שכנים')
P('מדד מרחק: cosine distance = 1 − cosine_similarity. מימוש: C עם heap (min/max), xorshift64 RNG.', bold=True)
P('נימוק לבחירה: HNSW עדיף על k-d Tree (שמתפרק ב-768 ממדים) ועל LSH (דיוק נמוך יותר). הוא ה-state-of-the-art לחיפוש ANN.')
doc.add_paragraph()

H(2,'9.2 BPR — סינון שיתופי (אות 2: Collaborative)')
P('BPR (Bayesian Personalized Ranking) פורסם ב-2009 על ידי Rendle et al. הוא לומד מ-implicit feedback — "המשתמש אינטראקט עם מאמר X אבל לא עם מאמר Y, לכן X > Y".')
P('מודל: מטריצת פירוק. לכל משתמש u יש וקטור U_u ∈ R^F, לכל פריט i יש V_i ∈ R^F. ציון: score(u,i) = U_u · V_i.', bold=True)
P('אימון (bpr_fit) — SGD עם דגימה שלילית:', bold=True)
C('FOR epoch = 1 TO num_epochs:')
C('    shuffle pairs (Fisher-Yates)')
C('    FOR each (user, pos_item) in pairs:')
C('        neg_item = random item (uniform)')
C('        x_uij = dot(U_user, V_pos) - dot(U_user, V_neg)')
C('        grad = 1 - sigmoid(x_uij)')
C('        U_user += lr * (grad * (V_pos - V_neg) - reg * U_user)')
C('        V_pos  += lr * (grad * U_user - reg * V_pos)')
C('        V_neg  += lr * (-grad * U_user - reg * V_neg)')
P('נימוק: BPR מתאים ל-implicit feedback. ALS מיועד ל-explicit ratings. SVD++ מורכב יותר למימוש ללא יתרון משמעותי במקרה שלנו.')
doc.add_paragraph()

H(2,'9.3 BM25 — חיפוש טקסט (אות 3: Text Search)')
P('BM25 (Robertson & Zaragoza, 2009) — הסטנדרט לחיפוש טקסט. נוסחת הציון:', bold=True)
C('BM25(d, Q) = Σ_{t ∈ Q}  IDF(t) · tf(t,d)·(k1+1) / (tf(t,d) + k1·(1 − b + b·|d|/avgdl))')
C('IDF(t) = ln((N − df(t) + 0.5) / (df(t) + 0.5) + 1)')
P('כאשר: N = מספר מסמכים, df(t) = מספר מסמכים שמכילים t, tf(t,d) = מספר הופעות של t ב-d, avgdl = אורך מסמך ממוצע.')
P('k1=1.2 שולט על רוויית TF — תדירות גבוהה לא גדלה ללא גבול. b=0.75 שולט על נורמליזציה לאורך — מסמכים ארוכים "נענשים" קלות.')
P('נימוק: TF-IDF לא מנרמל לאורך מסמך — מאמרים עם abstract ארוך יקבלו ציון גבוה מדי. BM25 פותר זאת.')
doc.add_paragraph()

H(2,'9.4 PageRank — דירוג גרף ציטוטים (אות 4: Graph)')
P('PageRank מדרג צמתים בגרף מכוון. הנוסחה האיטרטיבית:', bold=True)
C('PR(p) = (1-d)/N + d · Σ_{q ∈ B(p)} PR(q)/L(q)')
P('כאשר d=0.85 (damping), B(p) = צמתים שמצביעים על p, L(q) = דרגת יציאה של q.')
P('טיפול בצמתים תלויים (dangling nodes): צמתים ללא קשתות יוצאות מחלקים את הניקוד שלהם באחידות לכל הצמתים.')
P('בפרויקט: 20 איטרציות. מספיק להתכנסות עבור גרפים בסדר גודל של מאות-אלפים בודדים.')
doc.add_paragraph()

H(2,'9.5 RRF + MMR — מיזוג וגיוון')
P('RRF:', bold=True)
C('score(d) = Σ_{r ∈ rankings} 1 / (k + rank_r(d) + 1),  k=60')
P('MMR:', bold=True)
C('next = argmax_{d ∈ R\\S} [λ · rel(d) - (1-λ) · max_{s ∈ S} cosine_sim(emb_d, emb_s)]')
C('λ = 0.7')
P('λ=0.7 נבחר כי: 0.5 נותן יותר מדי גיוון על חשבון רלוונטיות, 1.0 לא מגוון כלל. 0.7 הוא ערך סטנדרטי בספרות.')
doc.add_paragraph()

H(2,'9.6 פרופיל משתמש — דעיכה אקספוננציאלית')
P('וקטור הפרופיל מחושב בזמן אמת (לא נשמר ב-DB):')
C('FOR each interaction (paper, action, timestamp):')
C('    days = (now - timestamp) / 86400')
C('    w = action_weight * exp(-ln(2)/30 * days)')
C('    profile += w * paper.embedding')
C('profile /= total_weight')
P('חצי-חיים = 30 ימים. משקלות: VIEW=0.2, CLICK=0.5, SAVE=1.0, DOWNLOAD=1.5, CITE=3.0.')
PB()

########################################
# 10. ARCHITECTURE
########################################
H(1,'10. ארכיטקטורה — Top-Down Level Design')
P('המערכת בנויה בהפרדת שכבות (MVC) עם 7 רכיבים היררכיים:')
T(['תיאור','חבילות','שכבה'],[
    ['ממשק HTTP REST — מקבל בקשות ומחזיר JSON','Controllers, DTOs, GlobalExceptionHandler','1. Presentation (web/)'],
    ['לוגיקה עסקית — אורקסטרציה של אלגוריתמים','RecommendationService, IndexService, IngestService, PaperService, UserService, InteractionService','2. Business Logic (service/)'],
    ['גישה ל-PostgreSQL דרך JPA/Hibernate','Entities, Repositories, FloatArrayConverter','3. Data Access (db/)'],
    ['מודלי נתונים טהורים','Paper, User, Interaction','4. Domain Model (model/)'],
    ['אלגוריתמים ב-Java','Bm25Index, FusionReranker, MmrReranker','5. Algorithm (algorithm/)'],
    ['גשר JNI לספריות C','HnswNative, BprNative, PageRankNative, NativeLoader','6. Native Bridge (jni/)'],
    ['לקוחות API חיצוניים — קבלת נתונים בלבד','SemanticScholarClient, ArxivClient, OpenAlexClient','7. Integration (api/)'],
])
doc.add_paragraph()
PLACEHOLDER('יש להכניס כאן את תרשים הארכיטקטורה Top-Down. קוד PlantUML להפקת התרשים מצורף בסוף המסמך')
PB()

########################################
# 11. USE CASES
########################################
H(1,'11. תרשים מקרי-שימוש — UML Use Cases')
PLACEHOLDER('יש להכניס כאן תרשים Use Case UML. קוד PlantUML מצורף בסוף המסמך')
doc.add_paragraph()
P('שחקנים (Actors):', bold=True)
B('משתמש (חוקר/סטודנט) — מחפש ומקבל המלצות')
B('מנהל (Admin) — מנהל נתונים ואינדקסים')
B('מערכת חיצונית (Semantic Scholar / ArXiv) — מספקת נתונים')
doc.add_paragraph()
T(['תיאור','שחקן','מקרה שימוש'],[
    ['המשתמש מזין userId ו-query אופציונלי. המערכת מריצה 7 שלבים ומחזירה K מאמרים','משתמש','UC-01: קבלת המלצות'],
    ['לחיצה, צפייה (2 שניות), שמירה, Add to refs — נרשמות אוטומטית ב-DB עם משקל','משתמש','UC-02: רישום אינטראקציה'],
    ['שאילתת חיפוש חופשית → BM25 מדרג','משתמש','UC-03: חיפוש טקסט'],
    ['חיפוש ב-S2/ArXiv, שמירה ל-DB, reindex אוטומטי','מנהל','UC-04: אכילת מאמרים'],
    ['מאמר + כל הפניותיו + קשתות ציטוט','מנהל','UC-05: Deep Ingest'],
    ['בנייה מחדש של HNSW + BM25 + BPR + PageRank','מנהל','UC-06: Reindex'],
    ['papersInDb, embeddingCount, bm25Docs, bprTrained','מנהל','UC-07: סטטיסטיקות'],
])
PB()

########################################
# 12. DATA STRUCTURES
########################################
H(1,'12. מבנה נתונים')
H(2,'12.1 Paper')
T(['טיפוס','שדה','תיאור'],[
    ['UUID','id','מזהה ראשי (PK)'],['String','s2Id, arxivId, doi','מזהים חיצוניים (unique index)'],
    ['String','title','כותרת המאמר'],['String','abstractText','תקציר'],['int','year','שנת פרסום'],
    ['List<String>','topics','נושאי מחקר (@ElementCollection → paper_topics)'],
    ['List<String>','authorNames','מחברים (@ElementCollection → paper_authors)'],
    ['float[768]','embedding','וקטור SPECTER2 — bytea, little-endian float32 (FloatArrayConverter)'],
    ['Set<UUID>','citedPaperIds','מזהי מאמרים מצוטטים → paper_citations'],
])
doc.add_paragraph()
H(2,'12.2 Interaction')
T(['טיפוס','שדה','תיאור'],[
    ['UUID','userId','FK למשתמש'],['UUID','paperId','FK למאמר'],
    ['ActionType (Enum)','actionType','VIEW=0.2, CLICK=0.5, SAVE=1.0, DOWNLOAD=1.5, CITE=3.0'],
    ['Instant','createdAt','חותמת זמן'],
])
doc.add_paragraph()
H(2,'12.3 מבני נתונים נייטיבים (C)')
T(['תיאור','שדות','מבנה'],[
    ['גרף HNSW עם מערך נודים דינמי','dim, M, M0, ef_construction, mL, nodes[], entry_point, max_level, rng_state','HnswIndex'],
    ['צומת בגרף — וקטור + שכנים לכל שכבה','vec[], level, layers[] (HnswNeighbors)','HnswNode'],
    ['תור עדיפויות (min/max) לחיפוש','data[] (HnswEntry: id + dist), size, cap, is_max','HnswHeap'],
    ['מטריצות פירוק','user_factors[], item_factors[], num_users, num_items, num_factors, rng_state','BprModel'],
])
PB()

########################################
# 13. ENVIRONMENT
########################################
H(1,'13. סביבת העבודה ושפת התכנות')
T(['ערך','פרט'],[
    ['Java 17','שפה ראשית'],['C (ANSI C99)','שפה משנית — אלגוריתמים ביצועיים (JNI)'],
    ['IntelliJ IDEA','סביבת פיתוח'],['Spring Boot 3.2.5','מסגרת אפליקציה'],
    ['Maven','כלי בנייה (Java)'],['CMake','כלי בנייה (C)'],
    ['PostgreSQL 16 (Neon Cloud, eu-central-1)','מסד נתונים'],['Flyway','מיגרציות DB'],
    ['HikariCP (pool=5)','מאגר חיבורים'],['8080','פורט'],['Windows','מערכת הפעלה'],
])
PB()

########################################
# 14. MAIN ALGORITHM PSEUDOCODE
########################################
H(1,'14. אלגוריתם ראשי (פסאודו קוד)')
H(2,'14.1 Recommend — צינור ההמלצה המלא')
C('FUNCTION Recommend(userId, textQuery, k):')
C('    candidates = k * 5')
C('    seen = InteractionService.getSeenPaperIds(userId)')
C('')
C('    // Signal 1: Content-Based (HNSW)')
C('    profile = UserProfileVector(userId, history)')
C('    IF profile != NULL:')
C('        hnswRanking = IndexService.searchHnsw(profile, candidates)')
C('')
C('    // Signal 2: Collaborative (BPR)')
C('    IF userId != NULL AND bprTrained:')
C('        bprRanking = IndexService.recommendBpr(userId, candidates, seen)')
C('')
C('    // Signal 3: Text Search (BM25)')
C('    IF textQuery != NULL AND textQuery != "":')
C('        tokens = Tokenizer.tokenize(textQuery)')
C('        bm25Ranking = IndexService.searchBm25(tokens, candidates)')
C('')
C('    // Signal 4: Graph (PageRank)')
C('    prRanking = IndexService.topByPageRank(candidates)')
C('')
C('    // Collect non-empty rankings')
C('    rankings = [hnswRanking, bprRanking, bm25Ranking, prRanking]')
C('    rankings = filter(not empty, rankings)')
C('    IF rankings is empty: RETURN []')
C('')
C('    // Fusion: Reciprocal Rank Fusion')
C('    fused = RRF(rankings, k=60)')
C('    // score(d) = Sigma 1/(60 + rank(d) + 1) for each ranking')
C('')
C('    // Build relevance map for MMR')
C('    FOR rank = 0 TO |fused|:')
C('        relevance[fused[rank]] = 1.0 / (1 + rank)')
C('')
C('    // Diversity: MMR reranking')
C('    reranked = MMR(fused, relevance, embeddings, lambda=0.7, k*2)')
C('')
C('    // Exclude seen papers, convert idx -> UUID, trim to k')
C('    result = []')
C('    FOR idx IN reranked:')
C('        uuid = paperIdxToUuid(idx)')
C('        IF uuid NOT IN seen:')
C('            result.add(uuid)')
C('        IF |result| >= k: BREAK')
C('    RETURN result')

doc.add_paragraph()
H(2,'14.2 UserProfileVector — פרופיל משתמש')
C('FUNCTION UserProfileVector(userId, interactions):')
C('    profile = float[768] initialized to 0')
C('    total = 0.0')
C('    lambda = ln(2) / 30.0  // half-life = 30 days')
C('    FOR each interaction IN interactions:')
C('        idx = paperUuidToIdx(interaction.paperId)')
C('        IF idx == NULL: CONTINUE')
C('        emb = embeddings[idx]')
C('        IF emb == NULL: CONTINUE')
C('        daysSince = (now - interaction.createdAt) / 86400')
C('        w = interaction.weight * exp(-lambda * daysSince)')
C('        FOR i = 0 TO 767:')
C('            profile[i] += w * emb[i]')
C('        total += w')
C('    IF total == 0: RETURN NULL')
C('    FOR i = 0 TO 767: profile[i] /= total')
C('    RETURN profile')

doc.add_paragraph()
H(2,'14.3 HNSW Search — חיפוש וקטורי')
C('FUNCTION hnsw_search(index, query, k, ef):')
C('    ep = index.entry_point')
C('    // Phase 1: Greedy descent from top layer to layer 1')
C('    FOR layer = max_level DOWN TO 1:')
C('        W = search_layer(index, query, ep, ef=1, layer)')
C('        ep = nearest node in W')
C('    // Phase 2: Full beam search at layer 0')
C('    W = search_layer(index, query, ep, ef, layer=0)')
C('    RETURN k nearest from W (sorted by cosine distance)')
C('')
C('FUNCTION search_layer(index, query, ep, ef, layer):')
C('    W = max-heap (furthest at top)')
C('    candidates = min-heap')
C('    push(W, ep); push(candidates, ep); visited[ep] = true')
C('    WHILE candidates not empty:')
C('        c = pop(candidates)  // closest unvisited')
C('        IF dist(c) > dist(top(W)): BREAK')
C('        FOR each neighbor n of c at layer:')
C('            IF visited[n]: CONTINUE')
C('            visited[n] = true')
C('            d = cosine_dist(query, n.vec)')
C('            IF d < dist(top(W)) OR |W| < ef:')
C('                push(W, n); push(candidates, n)')
C('                IF |W| > ef: pop(W)  // evict furthest')
C('    RETURN W')

doc.add_paragraph()
H(2,'14.4 BPR Fit — אימון סינון שיתופי')
C('FUNCTION bpr_fit(model, users[], pos_items[], n_pairs, epochs, lr, reg):')
C('    order = [0, 1, ..., n_pairs-1]')
C('    FOR epoch = 1 TO epochs:')
C('        Fisher-Yates shuffle(order)')
C('        FOR k = 0 TO n_pairs-1:')
C('            u = users[order[k]]')
C('            i = pos_items[order[k]]')
C('            j = random(0, num_items-1)  // negative sample')
C('            IF j == i: j = (j+1) % num_items')
C('            x_uij = dot(U[u], V[i]) - dot(U[u], V[j])')
C('            grad = 1.0 - sigmoid(x_uij)')
C('            // SGD update with L2 regularization')
C('            FOR f = 0 TO num_factors-1:')
C('                diff = V[i][f] - V[j][f]')
C('                U[u][f] += lr * (grad * diff - reg * U[u][f])')
C('                V[i][f] += lr * (grad * U[u][f] - reg * V[i][f])')
C('                V[j][f] += lr * (-grad * U[u][f] - reg * V[j][f])')
PB()

########################################
# 15. EXTERNAL INTERFACES / API
########################################
H(1,'15. תיאור ממשקים חיצוניים (גרפיקה) — תיאור API')
T(['תיאור','פרמטרים','Method','Endpoint'],[
    ['המלצות מותאמות אישית','userId?, q?, k=10','GET','/api/recommend'],
    ['חיפוש ב-Semantic Scholar ושמירה','query, limit=20','POST','/api/ingest/s2'],
    ['Deep ingest: מאמר + הפניות + ציטוטים','maxRefs=50','POST','/api/ingest/s2/{id}'],
    ['חיפוש ב-ArXiv','query, maxResults=20','POST','/api/ingest/arxiv'],
    ['Rebuild כל האינדקסים','—','POST','/api/admin/reindex'],
    ['Bulk deep ingest','maxRefs=20, delayMs=400','POST','/api/admin/deep-ingest-all'],
    ['סטטיסטיקות','—','GET','/api/admin/stats'],
    ['דיבאג BM25','q','GET','/api/admin/search-debug'],
    ['CRUD מאמרים','—','GET/POST','/api/papers'],
    ['CRUD משתמשים','—','GET/POST','/api/users'],
    ['רישום אינטראקציה','userId, paperId, actionType','POST','/api/interactions'],
])
doc.add_paragraph()
P('תיאור ממשק משתמש:', bold=True)
PLACEHOLDER('יש להכניס כאן צילום מסך של הממשק הראשי (index.html) — חיפוש + כרטיסי מאמרים')
PLACEHOLDER('יש להכניס כאן צילום מסך של קונסול הניהול (admin.html) — כפתורי Reindex, Deep Ingest, Stats')
PB()

########################################
# 16. CLASS DIAGRAM
########################################
H(1,'16. תרשים מחלקות — UML Class Diagram')
PLACEHOLDER('יש להכניס כאן תרשים UML Class Diagram. קוד PlantUML מצורף בסוף המסמך')
PB()

########################################
# 17. FUNCTION TABLES
########################################
H(1,'17. הפונקציות הראשיות בפרויקט')
P('עבור כל מחלקה מצורפת טבלה המתארת את הפונקציות החשובות, כולל פרמטרים, ערך חזרה, תיאור, יעילות ופסאודו קוד.')
doc.add_paragraph()

H(2,'17.1 מחלקת Bm25Index')
T(['פסאודו קוד','יעילות','תיאור','מחזירה','פרמטרים','פונקציה'],[
    ['FOR each doc: count TF per term. compute IDF. build inverted index.','O(N·D)','בונה אינדקס הפוך מכל המסמכים','void','(none)','build()'],
    ['FOR each query term: get postings. compute BM25 score. sort desc.','O(Q·df + N·log N)','מדרג כל מסמכים ומחזיר top-k','List<Result>','List<String> queryTerms, int k','search()'],
    ['IF docId != documents.size: throw. add to forward index.','O(|tokens|)','מוסיף מסמך בודד','void','int docId, List<String> tokens','addDocument()'],
    ['sum IDF*TF_norm for matching terms in doc','O(Q·df)','ציון BM25 למסמך','double','int docId, List<String> q','scoreDocument()'],
])
doc.add_paragraph()

H(2,'17.2 מחלקת FusionReranker')
T(['פסאודו קוד','יעילות','תיאור','מחזירה','פרמטרים','פונקציה'],[
    ['FOR each ranking: FOR each item at rank r: scores[item] += 1/(k+r+1). sort desc.','O(R·n + n·log n)','מיזוג RRF','List<Long>','List<List<Long>> rankings, int k','reciprocalRankFusion()'],
])
doc.add_paragraph()

H(2,'17.3 מחלקת MmrReranker')
T(['פסאודו קוד','יעילות','תיאור','מחזירה','פרמטרים','פונקציה'],[
    ['WHILE |selected| < k: FOR each remaining: compute λ*rel - (1-λ)*maxSim. pick best.','O(k·|R|·dim)','גיוון MMR','List<Long>','List<Long> cands, Map rel, Map embs, double λ, int k','rerank()'],
    ['dot=0; FOR i: dot+=a[i]*b[i]; normA+=a[i]^2; normB+=b[i]^2. return dot/sqrt(normA*normB)','O(dim)','דמיון קוסינוס','double','float[] a, float[] b','cosineSimilarity()'],
])
doc.add_paragraph()

H(2,'17.4 מחלקת IndexService')
T(['פסאודו קוד','יעילות','תיאור','מחזירה','פרמטרים','פונקציה'],[
    ['AtomicBoolean guard. load papers+users. build HNSW, BM25, BPR, PageRank.','O(N·M·log N)','בנייה מחדש של כל האינדקסים','void','(none)','reindex()'],
    ['JNI call: HnswNative.search(ptr, query, k, ef)','O(ef·M·log N)','חיפוש k-NN','List<Integer>','float[] q, int k','searchHnsw()'],
    ['bm25Index.search(tokens, k)','O(Q·df)','חיפוש BM25','List<Integer>','List<String> tokens, int k','searchBm25()'],
    ['JNI call: BprNative.recommend(ptr, userIdx, exclude, k)','O(I·F)','BPR top-k','List<Integer>','UUID userId, int k, Set<UUID> seen','recommendBpr()'],
    ['weighted avg of embeddings with exp decay','O(|history|·768)','פרופיל משתמש','float[768]','UUID, List<Interaction>','userProfileVector()'],
])
doc.add_paragraph()

H(2,'17.5 מחלקת RecommendationService')
T(['פסאודו קוד','יעילות','תיאור','מחזירה','פרמטרים','פונקציה'],[
    ['7 steps: profile→HNSW→BPR→BM25→PR→RRF→MMR→exclude seen','O(k·M·log N)','צינור ההמלצה המלא','List<UUID>','UUID userId, String q, int k','recommend()'],
    ['findAllById(ids). map to DTO inside @Transactional.','O(k)','טעינת מאמרים ל-DTO','List<PaperDto>','List<UUID> ids','fetchPapers()'],
])
doc.add_paragraph()

H(2,'17.6 פונקציות C נייטיביות')
T(['פסאודו קוד','יעילות','תיאור','מחזירה','פרמטרים','פונקציה'],[
    ['sample level. greedy descent. search_layer. bidirectional edges. prune.','O(M·log N·ef)','הוספת וקטור ל-HNSW','int (nodeId)','HnswIndex*, float[] vec','hnsw_add()'],
    ['greedy to layer 1. beam at layer 0. select k nearest.','O(ef·M·log N)','חיפוש k-NN','int (count)','HnswIndex*, float[] q, int k, int ef','hnsw_search()'],
    ['Fisher-Yates. FOR each pair: neg sample. sigmoid. SGD update.','O(epochs·n·F)','אימון BPR','void','BprModel*, arrays, epochs, lr, reg','bpr_fit()'],
    ['score all items. exclude. partial selection sort top-k.','O(I·F + k·I)','המלצות BPR','void','BprModel*, int user, exclude, k','bpr_recommend()'],
    ['count out-deg. iterate: propagate + dangling + teleport.','O(iter·(E+N))','PageRank','void','arrays, edges, nodes, d, iter','pagerank_compute()'],
])
PB()

########################################
# 18. MAIN PROGRAM
########################################
H(1,'18. התוכנית הראשית')
P('המחלקה המנהלת את כל הפרויקט היא PaperRecommenderApplication.java (Spring Boot entry point). זרימת ההפעלה:')
C('PROGRAM PaperRecommenderApplication:')
C('    SpringApplication.run(PaperRecommenderApplication.class)')
C('    // Spring Boot initializes: DB connection, JPA, Flyway migration')
C('')
C('    DataLoader.run():  // ApplicationRunner')
C('        IF paperRepository.count() >= 10: SKIP')
C('        ELSE:')
C('            FOR topic IN ["neural networks", "machine learning",')
C('                          "computer vision", "information retrieval",')
C('                          "graph algorithms"]:')
C('                ingestService.ingestFromSemanticScholar(topic, limit=8)')
C('            indexService.reindex()')
C('')
C('    IndexWarmUpRunner.run():  // ApplicationRunner')
C('        indexService.reindex()')
C('        // Builds: HNSW + BM25 + BPR + PageRank')
C('')
C('    // Embedded Tomcat serves on port 8080:')
C('    //   static/index.html  -> User UI')
C('    //   static/admin.html  -> Admin console')
C('    //   /api/**             -> REST endpoints')
C('')
C('    // On each HTTP request:')
C('    //   GET /api/recommend?userId=...&q=...&k=10')
C('    //     -> RecommendController.recommend()')
C('    //     -> RecommendationService.recommend()')
C('    //     -> [7 pipeline steps]')
C('    //     -> RecommendationService.fetchPapers()')
C('    //     -> JSON response')
PB()

########################################
# 19. USER GUIDE
########################################
H(1,'19. מדריך למשתמש')
H(2,'19.1 התקנה')
N('לפתוח את הפרויקט ב-IntelliJ IDEA')
N('לוודא ש-Java 17 מותקנת ומוגדרת כ-SDK של הפרויקט')
N('לבנות את ספריות ה-C הנייטיביות: cmake -S native/ -B native/build && cmake --build native/build')
N('לוודא שקבצי ה-DLL הועתקו ל-src/main/resources/native/')
N('ללחוץ על Run ב-IntelliJ')
H(2,'19.2 הפעלה ראשונית')
N('בהפעלה הראשונה, DataLoader זורע כ-40 מאמרים מ-Semantic Scholar (5 נושאים × 8 מאמרים)')
N('להריץ deep ingest לאיכלוס ציטוטים ונושאים: POST /api/admin/deep-ingest-all?maxRefs=20&delayMs=400')
N('הממשק זמין ב-http://localhost:8080')
H(2,'19.3 ממשק המשתמש')
PLACEHOLDER('יש להכניס כאן צילום מסך של מסך החיפוש הראשי')
B('שדה חיפוש — הקלדת שאילתה חופשית ולחיצה על Search')
B('כרטיסי מאמרים — כותרת, תקציר (מקוצר), שנה, נושאים, מחברים')
B('כפתור Save — שמירת מאמר לרשימה האישית')
B('כפתור Add to refs — הוספה לרשימת ההפניות')
B('אינטראקציות אוטומטיות: CLICK ברגע לחיצה על כרטיס, VIEW אחרי 2 שניות של צפייה')
H(2,'19.4 קונסול ניהול')
PLACEHOLDER('יש להכניס כאן צילום מסך של קונסול הניהול (admin.html)')
B('כפתור Reindex — בנייה מחדש של כל האינדקסים')
B('כפתור Deep Ingest All — אכילת עומק של כל המאמרים')
B('Stats — מספר מאמרים, embeddings, docs ב-BM25, האם BPR אומן')
B('Search Debug — הצגת טוקניזציה ותוצאות BM25 לדיבאג')
PB()

########################################
# 20. REFLECTION
########################################
H(1,'20. סיכום אישי / רפלקציה')
PLACEHOLDER('חלק זה חשוב ביותר ואין להקל בו ראש. יש למלא באופן אישי — לפחות חצי עמוד עד עמוד שלם. נקודות מנחות:')
doc.add_paragraph()
B('כיצד הייתה עבורך העבודה על הפרויקט?')
B('מה קיבלת מהפרויקט? אילו כלים אתה לוקח איתך להמשך?')
B('מה הקשיים / אתגרים שעמדו בפניך? (למשל: מימוש HNSW ב-C, ניהול זיכרון ב-JNI, דיבאג נייטיב)')
B('מה המסקנות שלך?')
B('מה היית עושה אחרת לו היית מתחיל היום?')
B('מה היית משנה בתהליך העבודה כדי שתהיה יעילה יותר?')
PB()

########################################
# 21. BIBLIOGRAPHY (APA)
########################################
H(1,'21. ביבליוגרפיה')
P('מקורות לפי כללי APA:')
refs=[
    'Brin, S., & Page, L. (1998). The anatomy of a large-scale hypertextual web search engine. Computer Networks and ISDN Systems, 30(1-7), 107-117.',
    'Carbonell, J., & Goldstein, J. (1998). The use of MMR, diversity-based reranking for reordering documents and producing summaries. Proceedings of the 21st Annual International ACM SIGIR Conference, 335-336.',
    'Cohan, A., Feldman, S., Beltagy, I., Downey, D., & Weld, D. S. (2020). SPECTER: Document-level representation learning using citation-informed transformers. Proceedings of ACL, 2270-2282.',
    'Cormack, G. V., Clarke, C. L. A., & Buettcher, S. (2009). Reciprocal rank fusion outperforms Condorcet and individual rank learning methods. Proceedings of the 32nd International ACM SIGIR Conference, 758-759.',
    'Malkov, Y. A., & Yashunin, D. A. (2020). Efficient and robust approximate nearest neighbor search using Hierarchical Navigable Small World graphs. IEEE Transactions on Pattern Analysis and Machine Intelligence, 42(4), 824-836.',
    'Rendle, S., Freudenthaler, C., Gantner, Z., & Schmidt-Thieme, L. (2009). BPR: Bayesian personalized ranking from implicit feedback. Proceedings of the 25th Conference on Uncertainty in Artificial Intelligence, 452-461.',
    'Robertson, S. E., & Zaragoza, H. (2009). The probabilistic relevance framework: BM25 and beyond. Foundations and Trends in Information Retrieval, 3(4), 333-389.',
]
for ref in refs:
    p=doc.add_paragraph()
    p.paragraph_format.left_indent=Cm(1.27);p.paragraph_format.first_line_indent=Cm(-1.27)
    p.paragraph_format.space_after=Pt(6)
    r=p.add_run(ref);r.font.name='David';r.font.size=Pt(11)
PB()

########################################
# 22. APPENDIX A — CODE
########################################
H(1,'22. נספח א׳ — קוד הפרויקט')
P('להלן קוד המחלקות המרכזיות כטקסט (לא כתמונה):')

code_files=[
    ('algorithm/bm25/Bm25Index.java','src/main/java/com/recommender/algorithm/bm25/Bm25Index.java'),
    ('algorithm/rerank/FusionReranker.java','src/main/java/com/recommender/algorithm/rerank/FusionReranker.java'),
    ('algorithm/rerank/MmrReranker.java','src/main/java/com/recommender/algorithm/rerank/MmrReranker.java'),
    ('service/RecommendationService.java','src/main/java/com/recommender/service/RecommendationService.java'),
    ('service/IndexService.java','src/main/java/com/recommender/service/IndexService.java'),
    ('model/Paper.java','src/main/java/com/recommender/model/Paper.java'),
    ('model/Interaction.java','src/main/java/com/recommender/model/Interaction.java'),
    ('service/util/Tokenizer.java','src/main/java/com/recommender/service/util/Tokenizer.java'),
    ('native/hnsw/hnsw.c','native/hnsw/hnsw.c'),
    ('native/bpr/bpr.c','native/bpr/bpr.c'),
    ('native/pagerank/pagerank.c','native/pagerank/pagerank.c'),
]
for label,path in code_files:
    H(2,label)
    src=code_file(path)
    for line in src.split('\n'):
        C(line)
    doc.add_paragraph()
PB()

########################################
# 23. APPENDIX B — PLANTUML CODE
########################################
H(1,'23. נספח ב׳ — קוד PlantUML לתרשימים')
P('יש להריץ את הקוד הבא באתר plantuml.com או בפלאגין PlantUML ב-IntelliJ, ולהכניס את התמונות שמתקבלות במקומות המסומנים במסמך.')
doc.add_paragraph()

H(2,'תרשים Use Case')
C('@startuml')
C('left to right direction')
C('actor "משתמש" as User')
C('actor "מנהל" as Admin')
C('actor "Semantic Scholar" as S2')
C('actor "ArXiv" as Arxiv')
C('')
C('rectangle "PaperRecommender" {')
C('  usecase "קבלת המלצות" as UC1')
C('  usecase "חיפוש טקסט" as UC2')
C('  usecase "רישום אינטראקציה" as UC3')
C('  usecase "שמירת מאמר" as UC4')
C('  usecase "אכילת מאמרים" as UC5')
C('  usecase "Deep Ingest" as UC6')
C('  usecase "Reindex" as UC7')
C('  usecase "סטטיסטיקות" as UC8')
C('}')
C('')
C('User --> UC1')
C('User --> UC2')
C('User --> UC3')
C('User --> UC4')
C('Admin --> UC5')
C('Admin --> UC6')
C('Admin --> UC7')
C('Admin --> UC8')
C('UC5 --> S2')
C('UC5 --> Arxiv')
C('UC6 --> S2')
C('@enduml')

doc.add_paragraph()
H(2,'תרשים Class Diagram')
C('@startuml')
C('package "web" {')
C('  class RecommendController {')
C('    +recommend(userId, q, k): List<PaperDto>')
C('  }')
C('  class IngestController {')
C('    +ingestS2(query, limit)')
C('    +deepIngest(paperId, maxRefs)')
C('  }')
C('  class AdminController {')
C('    +reindex()')
C('    +stats(): Map')
C('  }')
C('}')
C('')
C('package "service" {')
C('  class RecommendationService {')
C('    -MMR_LAMBDA: 0.7')
C('    +recommend(userId, q, k): List<UUID>')
C('    +fetchPapers(ids): List<PaperDto>')
C('  }')
C('  class IndexService {')
C('    -hnswPtr: long')
C('    -bprPtr: long')
C('    -bm25Index: Bm25Index')
C('    -pageRankScores: double[]')
C('    +reindex()')
C('    +searchHnsw(q, k): List<Integer>')
C('    +searchBm25(tokens, k): List<Integer>')
C('    +recommendBpr(userId, k, seen): List<Integer>')
C('    +userProfileVector(userId, history): float[]')
C('  }')
C('  class IngestService {')
C('    +ingestFromSemanticScholar(query, limit)')
C('    +ingestPaperDeep(paperId, maxRefs)')
C('  }')
C('}')
C('')
C('package "algorithm" {')
C('  class Bm25Index {')
C('    -k1: 1.2, b: 0.75')
C('    -invertedIndex: Map<String,List<int[]>>')
C('    +addDocument(docId, tokens)')
C('    +build()')
C('    +search(queryTerms, k): List<Result>')
C('  }')
C('  class FusionReranker {')
C('    {static} +reciprocalRankFusion(rankings, k): List<Long>')
C('  }')
C('  class MmrReranker {')
C('    {static} +rerank(cands, rel, embs, lambda, k): List<Long>')
C('    {static} +cosineSimilarity(a, b): double')
C('  }')
C('}')
C('')
C('package "jni" {')
C('  class HnswNative <<native>> {')
C('    {static} +createIndex(dim, M, ef): long')
C('    {static} +addVector(ptr, vec)')
C('    {static} +search(ptr, q, k, ef): int[]')
C('  }')
C('  class BprNative <<native>>')
C('  class PageRankNative <<native>>')
C('}')
C('')
C('package "db" {')
C('  class PaperEntity {')
C('    -id: UUID')
C('    -embedding: float[768]')
C('    -topics: List<String>')
C('    -citedPaperIds: Set<UUID>')
C('  }')
C('  interface PaperRepository')
C('}')
C('')
C('package "model" {')
C('  class Paper')
C('  class User')
C('  class Interaction {')
C('    +ActionType: VIEW|CLICK|SAVE|DOWNLOAD|CITE')
C('  }')
C('}')
C('')
C('RecommendController --> RecommendationService')
C('RecommendationService --> IndexService')
C('RecommendationService --> FusionReranker')
C('RecommendationService --> MmrReranker')
C('IndexService --> HnswNative')
C('IndexService --> BprNative')
C('IndexService --> PageRankNative')
C('IndexService --> Bm25Index')
C('IndexService --> PaperRepository')
C('IngestController --> IngestService')
C('IngestService --> PaperRepository')
C('PaperRepository --> PaperEntity')
C('@enduml')

doc.add_paragraph()
H(2,'תרשים ארכיטקטורה Top-Down')
C('@startuml')
C('!define RECTANGLE class')
C('')
C('package "Presentation Layer" {')
C('  [RecommendController]')
C('  [IngestController]')
C('  [AdminController]')
C('  [PaperController]')
C('}')
C('')
C('package "Business Logic Layer" {')
C('  [RecommendationService]')
C('  [IndexService]')
C('  [IngestService]')
C('  [PaperService]')
C('}')
C('')
C('package "Algorithm Layer" {')
C('  [Bm25Index]')
C('  [FusionReranker]')
C('  [MmrReranker]')
C('}')
C('')
C('package "Native Layer (C/JNI)" {')
C('  [HNSW (C)]')
C('  [BPR (C)]')
C('  [PageRank (C)]')
C('}')
C('')
C('package "Data Access Layer" {')
C('  [PaperRepository]')
C('  [UserRepository]')
C('  [InteractionRepository]')
C('}')
C('')
C('database "PostgreSQL (Neon)" {')
C('  [papers]')
C('  [users]')
C('  [interactions]')
C('}')
C('')
C('[RecommendController] --> [RecommendationService]')
C('[RecommendationService] --> [IndexService]')
C('[RecommendationService] --> [FusionReranker]')
C('[RecommendationService] --> [MmrReranker]')
C('[IndexService] --> [Bm25Index]')
C('[IndexService] --> [HNSW (C)]')
C('[IndexService] --> [BPR (C)]')
C('[IndexService] --> [PageRank (C)]')
C('[IndexService] --> [PaperRepository]')
C('[PaperRepository] --> [papers]')
C('@enduml')

########################################
# SAVE
########################################
out=r'C:\Users\ikomostars\IdeaProjects\PaperRecommender\PaperRecommender_Book_HE_v2.docx'
doc.save(out)
print('OK:',out)
