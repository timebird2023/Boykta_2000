import os
import re
import sys
import json
import time
import base64
import random
import asyncio
import logging
import platform
import subprocess
import urllib.parse
import urllib.request
from datetime import datetime, timedelta

# ============================================
# 1. تثبيت المكتبات اللازمة
# ============================================
def install_libs():
    libs = ['aiohttp', 'aiosqlite']
    for lib in libs:
        try:
            __import__(lib)
        except ImportError:
            print(f"📦 جاري تثبيت المكتبة الناقصة: {lib}...", flush=True)
            subprocess.check_call([sys.executable, "-m", "pip", "install", lib])

install_libs()

import aiohttp
import aiosqlite
from aiohttp import web

logging.getLogger('aiohttp').setLevel(logging.ERROR)

# ============================================
# 2. الثوابت والإعدادات العامة
# ============================================
FB_PAGE_ACCESS_TOKEN = "EAAWMQZAFKMLYBRmFMXNCZAICHICqxxGwh3nlSZAnVmwiKcGFHCuz8GQ6CrQvFruZAZArJoGhkASzOU8VfDGGa0kZAxqFIEzrkN0v9wpD4ncTw5Tt3XqS32P5hhxnqlW5KSRp8LU5C8w74WGIvlZClUupAJff5zWVBPnwUWLoggxAjBu1E92TpH4NWpChZBRgZAAewDBhSfQZDZD"
FB_VERIFY_TOKEN = "my_secret_2025"
ADMIN_PSID = "26849940104706215"
PORT = 24852
PROXY_COOLDOWN = 180
CF_TOKEN = "eyJhIjoiOGNmMDQ1OGE1NTVlNTA2ZDRkMjQ1NDJmNDQ4YzM4NzUiLCJ0IjoiMDU1NDdjMzQtYWNkZi00ZDMwLTliYjEtYWZmOWNiN2NiOGQzIiwicyI6Ik56Z3lOR00yTlRndE1XSmxaaTAwWVRJMkxXSmlOV1l0TURNd09ERm1ObUUwWTJaaCJ9"

# ترسانة البروكسيات (المصححة)
PROXY_LIST = [
    "http://nusz54260-region-DZ:1ffrv8gx@us.1024proxy.io:3000",
    "http://g5oNHbtlKM60_custom_zone_DZ_st__city_sid_18810309_time_5:4554242@change6.owlproxy.com:7778",
    "http://Sf7MDO336C50_custom_zone_DZ_st__city_sid_14830140_time_5:4553959@change6.owlproxy.com:7778",
    "http://sn8wNFB3Ft20_custom_zone_DZ_st__city_sid_82486724_time_5:4553777@change6.owlproxy.com:7778",
    "http://HW7mFG5KE140_custom_zone_DZ_st__city_sid_89402255_time_5:4553227@change6.owlproxy.com:7778",
    "http://wRDydsNCVc40_custom_zone_DZ_st__city_sid_84043535_time_5:4555186@change6.owlproxy.com:7778",
    "http://A4nVRdWfo670_custom_zone_DZ_st__city_sid_58431096_time_5:4554877@change6.owlproxy.com:7778",
    "http://2JplKfk8QK80_custom_zone_DZ_st__city_sid_28486622_time_5:4629535@change6.owlproxy.com:7778",
    "http://hEnMcwCsai10_custom_zone_DZ_st__city_sid_66645007_time_5:4629732@change6.owlproxy.com:7778",
    "http://dovjjgFZOD80_custom_zone_DZ_st__city_sid_66283430_time_5:4630065@change4.owlproxy.com:7778",
    "http://gTjGimu89m10_custom_zone_DZ_st__city_sid_81601847_time_5:4630321@change6.owlproxy.com:7778",
    "http://QnhjXtcnSI10_custom_zone_DZ_st__city_sid_31141769_time_5:4898733@change6.owlproxy.com:7778",
    "http://pMb8KrVC0g70_custom_zone_DZ_st__city_sid_94618485_time_5:4898794@change6.owlproxy.com:7778",
    "http://2pSTAaZ2tj70_custom_zone_DZ_st__city_sid_21825431_time_5:4898851@change6.owlproxy.com:7778",
    "http://NSv16piOPQ70_custom_zone_DZ_st__city_sid_10120473_time_5:4898920@change6.owlproxy.com:7778",
    "http://aUsFWPrny700_custom_zone_DZ_st__city_sid_48071583_time_5:5039378@change6.owlproxy.com:7778",
    "https://bot626hu:iGFfkTNT@cdn.dexodata.com:443",
    "http://iDjXwknqnh:DTbvVdk0lh@proxy.proxyma.io:10000",
    "http://jekr4uOxyd:kRD70ZkF47@proxy.proxyma.io:10000",
    "http://om1P3VBkND:SlvbaKJpoM@proxy.proxyma.io:10000",
    "http://kMFwWUtIA3:sJO62zm8AB@proxy.proxyma.io:10000"
]

CLIENT_ID = "87pIExRhxBb3_wGsA5eSEfyATloa"
CLIENT_SECRET = "uf82p68Bgisp8Yg1Uz8Pf6_v1XYa"
BASE_URL = "https://apim.djezzy.dz/mobile-api"

HEADERS = {
    'User-Agent': "MobileApp/3.0.6",
    'Accept': "application/json",
    'Content-Type': "application/json",
    'accept-language': "ar"
}

CATALOG = {
    "OFFER_30_300M": {"price": "30", "data": "300 ميجابايت", "duration": "24 ساعة", "code": "DOVINTSPEEDDAY100MoPRE", "type": "shake", "emoji": "📦"},
    "OFFER_50_FB": {"price": "50", "data": "فيسبوك غير محدود", "duration": "4 ساعات", "code": "ImtiyazSurpriseData2hfbPRE", "type": "shake", "emoji": "📘"},
    "OFFER_70_4G": {"price": "70", "data": "4 جيجابايت", "duration": "24 ساعة", "code": "BTLINTSPEEDDAY2Go", "type": "shake", "emoji": "🟢"},
    "OFFER_70_3G_FB": {"price": "70", "data": "3 جيجابايت (فيسبوك)", "duration": "3 أيام", "code": "1GBFB3DAY", "type": "shake", "emoji": "🔵"},
    "OFFER_90_5GB": {"price": "90", "data": "5 جيجابايت", "duration": "24 ساعة", "code": "BTL500MBDAY", "type": "shake", "emoji": "🟣"},
    "OFFER_100_2G": {"price": "100", "data": "2 جيجابايت", "duration": "24 ساعة", "code": "DOVINTSPEEDDAY1GoPRE", "type": "shake", "emoji": "📦"},
    "OFFER_140_4G": {"price": "140", "data": "4 جيجابايت", "duration": "24 ساعة", "code": "BTL1GBDAY", "type": "shake", "emoji": "🔴"},
    "OFFER_150_4G_W": {"price": "150", "data": "4 جيجابايت", "duration": "7 أيام", "code": "DOVINTSPEEDWEEK2GoPRE", "type": "shake", "emoji": "📦"},
    "OFFER_190_10G": {"price": "190", "data": "10 جيجابايت", "duration": "3 أيام", "code": "BTL4GBDAY", "type": "shake", "emoji": "🟤"},
    "OFFER_250_3G_M": {"price": "250", "data": "3 جيجابايت", "duration": "شهر", "code": "2GBMONTH", "type": "shake", "emoji": "⚫"}
}

GLOBAL_SESSION = None
TASK_QUEUE = asyncio.Queue()
PROCESSING_USERS = set()
DB_FILE = 'boykta_fb.db'

# متغيرات الألعاب في الذاكرة الحية (RAM) للسرعة
WAITING_RPS = []
WAITING_PENALTIES = []
WAITING_SPLIT = []
ACTIVE_GAMES = {}
USER_GAME_MAP = {}
CHOICES_RPS = {'1': '🪨 حجر', '2': '📄 ورقة', '3': '✂️ مقص'}
CHOICES_PEN = {'1': '⬅️ يسار', '2': '⬆️ وسط', '3': '➡️ يمين'}
CHOICES_SPLIT = {'1': '🤝 تعاون', '2': '😈 خِداع'}

# ============================================
# 3. محرك تدوير البروكسي (Async - Fast Fail)
# ============================================
async def safe_request(method, url, **kwargs):
    proxies_to_try = random.sample(PROXY_LIST, min(5, len(PROXY_LIST)))
    t_val = kwargs.pop('timeout', 15)
    req_timeout = aiohttp.ClientTimeout(total=t_val)
    kwargs['ssl'] = False
    
    for proxy_url in proxies_to_try:
        try:
            if '@' in proxy_url:
                parsed = urllib.parse.urlparse(proxy_url)
                scheme = parsed.scheme
                host = parsed.hostname
                port_str = f":{parsed.port}" if parsed.port else ""
                user = urllib.parse.unquote(parsed.username) if parsed.username else None
                pwd = urllib.parse.unquote(parsed.password) if parsed.password else None
                kwargs['proxy'] = f"{scheme}://{host}{port_str}"
            else:
                scheme_part, rest = proxy_url.split('://', 1)
                parts = rest.split(':')
                scheme = scheme_part
                host = parts[0]
                port_str = f":{parts[1]}" if len(parts) > 1 else ""
                user = parts[2] if len(parts) > 2 else None
                pwd = parts[3] if len(parts) > 3 else None
                kwargs['proxy'] = f"{scheme}://{host}{port_str}"
            
            if user and pwd:
                auth_str = f"{user}:{pwd}"
                b64_auth = base64.b64encode(auth_str.encode()).decode()
                kwargs['proxy_headers'] = {'Proxy-Authorization': f"Basic {b64_auth}"}
            
            async with GLOBAL_SESSION.request(method, url, timeout=req_timeout, **kwargs) as response:
                text_data = await response.text()
                try: json_data = json.loads(text_data)
                except: json_data = None
                return {"status": response.status, "text": text_data, "json": json_data}
        except Exception:
            continue
            
    return {"status": 500, "text": "", "json": None, "error": "عذراً، واجهنا مشكلة في الاتصال بالخادم. يرجى المحاولة لاحقاً."}

# ============================================
# 4. محرك قاعدة البيانات (الاتصال الدائم - رامات 3GB)
# ============================================
DB_WRITE_LOCK = asyncio.Lock()
GLOBAL_DB = None

async def get_db():
    global GLOBAL_DB
    if GLOBAL_DB is None:
        GLOBAL_DB = await aiosqlite.connect(DB_FILE)
        await GLOBAL_DB.execute('PRAGMA journal_mode=WAL;')
        await GLOBAL_DB.execute('PRAGMA synchronous=NORMAL;')
    return GLOBAL_DB

async def db_query(query, params=(), fetchone=False, fetchall=False, commit=False):
    try:
        db = await get_db()
        if commit:
            async with DB_WRITE_LOCK:
                await db.execute(query, params)
                await db.commit()
                return True
        else:
            async with db.execute(query, params) as cursor:
                if fetchone: return await cursor.fetchone()
                if fetchall: return await cursor.fetchall()
    except Exception as e:
        print(f"❌ DB Error: {e}", flush=True)
        return None

async def init_db():
    db = await get_db()
    await db.execute('''CREATE TABLE IF NOT EXISTS users (
                    user_id TEXT PRIMARY KEY, state TEXT DEFAULT 'idle', phone TEXT,
                    last_otp_time REAL DEFAULT 0, otp_request_time REAL DEFAULT 0,
                    activation_start_time REAL DEFAULT 0, last_action_time REAL DEFAULT 0,
                    error_cooldown_until REAL DEFAULT 0, banned_until REAL DEFAULT 0,
                    is_new_user INTEGER DEFAULT 1, name TEXT, pending_offer_code TEXT,
                    points INTEGER DEFAULT 0
                )''')
    try: await db.execute("ALTER TABLE users ADD COLUMN points INTEGER DEFAULT 0")
    except: pass
    try: await db.execute("ALTER TABLE users ADD COLUMN link TEXT DEFAULT ''")
    except: pass
    
    await db.execute('''CREATE TABLE IF NOT EXISTS match_history (
                    user1 TEXT, user2 TEXT, date TEXT, play_count INTEGER DEFAULT 0,
                    PRIMARY KEY (user1, user2, date)
                )''')
                
    await db.execute('''CREATE TABLE IF NOT EXISTS tokens (
                    phone TEXT PRIMARY KEY, owner_id TEXT, access_token TEXT,
                    refresh_token TEXT, last_1gb_activation REAL DEFAULT 0, 
                    last_2gb_activation REAL DEFAULT 0, last_received_invite REAL DEFAULT 0
                )''')
    await db.execute('''CREATE TABLE IF NOT EXISTS verified_numbers (
                    user_id TEXT, phone TEXT, PRIMARY KEY(user_id, phone)
                )''')
    await db.execute('''CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)''')
    await db.execute('''CREATE TABLE IF NOT EXISTS scheduled_broadcasts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, send_time REAL,
                    message TEXT, status TEXT DEFAULT 'pending'
                )''')
    
    keys_to_init = ['maintenance', 'maintenance_msg', 'stat_walk_2gb', 'stat_free_sms']
    for o_key in CATALOG.keys(): keys_to_init.append(f'stat_offer_{o_key}')
    for k in keys_to_init:
        v = '0' if k != 'maintenance_msg' else 'عذراً، البوت تحت الصيانة حالياً. سنعود قريباً!'
        await db.execute("INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)", (k, v))
    
    await db.execute("UPDATE users SET state = 'idle'")
    await db.commit()

# ============================================
# 5. دوال مساعدة 
# ============================================
async def get_user_state(user_id):
    user = await db_query("SELECT user_id, state, phone, last_otp_time, otp_request_time, activation_start_time, last_action_time, error_cooldown_until, banned_until, is_new_user, name, pending_offer_code, points, link FROM users WHERE user_id = ?", (str(user_id),), fetchone=True)
    if not user:
        await db_query("INSERT OR IGNORE INTO users (user_id, name, is_new_user, link) VALUES (?, ?, 1, '')", (str(user_id), ""), commit=True)
        return {"state": "idle", "phone": None, "last_otp_time": 0, "otp_request_time": 0, "activation_start_time": 0, "last_action_time": 0, "error_cooldown_until": 0, "banned_until": 0, "is_new_user": 1, "name": "", "pending_offer_code": None, "points": 0, "link": ""}
        
    return {
        "state": user[1], "phone": user[2], "last_otp_time": user[3], "otp_request_time": user[4],
        "activation_start_time": user[5], "last_action_time": user[6], "error_cooldown_until": user[7],
        "banned_until": user[8], "is_new_user": user[9], "name": user[10], "pending_offer_code": user[11], "points": user[12], "link": user[13] if len(user) > 13 else ""
    }

async def update_user_state(user_id, **kwargs):
    if not kwargs: return
    query = "UPDATE users SET "
    updates = [f"{k} = ?" for k in kwargs.keys()]
    query += ", ".join(updates) + " WHERE user_id = ?"
    params = list(kwargs.values()) + [str(user_id)]
    await db_query(query, tuple(params), commit=True)

async def update_user_points(user_id, diff):
    if diff > 0:
        await db_query("UPDATE users SET points = points + ? WHERE user_id = ?", (diff, str(user_id)), commit=True)
    else:
        await db_query("UPDATE users SET points = MAX(0, points - ?) WHERE user_id = ?", (abs(diff), str(user_id)), commit=True)

def format_time(seconds):
    hours, rem = divmod(int(seconds), 3600)
    mins, secs = divmod(rem, 60)
    if hours > 0: return f"{hours} ساعة و {mins} دقيقة"
    return f"{mins} دقيقة و {secs} ثانية"

def get_masked_phone(phone):
    if not phone or len(phone) < 9: return "غير معروف"
    local_p = "0" + phone[-9:]
    return f"{local_p[:4]}XXXX{local_p[-2:]}"

# ============================================
# 6. دوال الفيسبوك والقوائم 
# ============================================
async def send_typing_indicator(recipient_id, state="typing_on"):
    url = f"https://graph.facebook.com/v17.0/me/messages?access_token={FB_PAGE_ACCESS_TOKEN}"
    try: await GLOBAL_SESSION.post(url, json={"recipient": {"id": recipient_id}, "sender_action": state}, timeout=5)
    except: pass

async def send_fb_message(recipient_id, text):
    url = f"https://graph.facebook.com/v17.0/me/messages?access_token={FB_PAGE_ACCESS_TOKEN}"
    try: await GLOBAL_SESSION.post(url, json={"recipient": {"id": recipient_id}, "message": {"text": text}}, timeout=5)
    except: pass

async def run_broadcast(admin_id, msg):
    await send_fb_message(admin_id, "⏳ جاري بدء الإذاعة الشاملة. الرجاء الانتظار...")
    users = await db_query("SELECT user_id FROM users", fetchall=True)
    if not users:
        await send_fb_message(admin_id, "⚠️ لا يوجد مستخدمون في قاعدة البيانات.")
        return
    count = 0
    for u in users:
        uid = u[0]
        if uid == admin_id: continue
        asyncio.create_task(send_fb_message(uid, f"📢 إشعار إداري:\n\n{msg}"))
        count += 1
        if count % 20 == 0: await asyncio.sleep(1)
    await send_fb_message(admin_id, f"✅ تمت الإذاعة بنجاح لـ {count} مستخدم.")

async def send_main_menu(user_id, phone, name, points):
    masked_p = get_masked_phone(phone)
    msg = (f"👤 الحساب: {masked_p} | {name}\n"
           f"⭐ رصيد النقاط: {points} نقطة\n🟢 الحالة: متصل\n\n"
           "👇 أرسل الرقم المناسب لاختيار الخدمة:\n\n"
           "1 ⬅️ 🏃 تفعيل باقة 2 جيجابايت (امشِ واربح)\n"
           "2 ⬅️ 💬 خدمات مجانية (كلمني / فليكسيلي)\n"
           "3 ⬅️ 🛒 متجر عروض الإنترنت\n"
           "4 ⬅️ 💳 رصيدي وباقاتي\n"
           "5 ⬅️ ⚙️ خدمات الشبكة\n"
           "6 ⬅️ 🎮 عالم الألعاب والتحديات\n"
           "7 ⬅️ 👤 ملفي الشخصي (تعديل الاسم أو الرابط)\n"
           "8 ⬅️ 🎵 إغلاق خدمة 'رناتي'\n\n"
           "🔄 لتغيير رقمك، يرجى كتابة الرقم الجديد مباشرة هنا.\n\n"
           "👍 لا تنسَ متابعة صفحتنا الرسمية [ boykta net ¹ ] ❤️")
    asyncio.create_task(send_fb_message(user_id, msg))

async def send_game_menu(user_id, points):
    msg = (f"🎮 مرحباً بك في عالم الألعاب! (رصيدك: {points} نقطة)\n\n"
           "1 ⬅️ 🪨📄✂️ لعبة حجر، ورقة، مقص (الأساسية)\n"
           "2 ⬅️ ⚽🧤 تحدي ضربات الترجيح (حماس التوقع)\n"
           "3 ⬅️ 💰😈 الشكارة والقلب الأسود (لعبة التعاون أو الخداع)\n"
           "4 ⬅️ 👑 قائمة أفضل اللاعبين (Leaderboard)\n\n"
           "0 ⬅️ 🔙 العودة للقائمة الرئيسية")
    asyncio.create_task(send_fb_message(user_id, msg))

async def send_free_sms_menu(user_id):
    msg = ("👇 قائمة الرسائل المجانية (أرسل الرقم المطلوب):\n\n"
           "1 ⬅️ 📞 إرسال رسالة (كلمني)\n"
           "2 ⬅️ 💸 إرسال رسالة (فليكسيلي)\n"
           "3 ⬅️ 📊 رصيد الرسائل المتبقي\n\n"
           "0 ⬅️ 🔙 العودة للقائمة الرئيسية")
    asyncio.create_task(send_fb_message(user_id, msg))

async def send_offers_menu(user_id):
    msg = "🛒 متجر عروض الإنترنت (أرسل الرقم للتفعيل):\n\n"
    i = 1
    for key, data in CATALOG.items():
        msg += f"{data['emoji']} {i} ⬅️ {data['data']} بـ {data['price']} د.ج ({data['duration']})\n"
        i += 1
    msg += "\n0 ⬅️ 🔙 العودة للقائمة الرئيسية"
    asyncio.create_task(send_fb_message(user_id, msg))

async def send_network_menu(user_id):
    msg = ("⚙️ خدمات الشبكة (أرسل الرقم المطلوب):\n\n"
           "1 ⬅️ 🕵️‍♂️ إخفاء رقمي (Appel Masqué)\n"
           "2 ⬅️ 👁️ إظهار رقمي (إلغاء الإخفاء)\n"
           "3 ⬅️ 📞 تفعيل انتظار المكالمات\n"
           "4 ⬅️ ⏱️ سجل تفعيلاتي الأخيرة\n\n"
           "0 ⬅️ 🔙 العودة للقائمة الرئيسية")
    asyncio.create_task(send_fb_message(user_id, msg))

async def show_leaderboard(user_id):
    rows = await db_query("SELECT name, points, link FROM users ORDER BY points DESC LIMIT 10", fetchall=True)
    if not rows:
        asyncio.create_task(send_fb_message(user_id, "الملعب فارغ حالياً، لا يوجد لاعبون في القائمة! العب الآن وكن في الصدارة."))
        return
    msg = "👑 قائمة أفضل اللاعبين (Top 10):\n\n"
    medals = ['🥇', '🥈', '🥉']
    for i, (name, points, link) in enumerate(rows):
        rank = medals[i] if i < 3 else f"{i+1}."
        msg += f"{rank} **{name}** ➖ {points} نقطة\n"
        if link and link.strip() != "":
            msg += f"🔗 الحساب: {link}\n"
        msg += "➖➖➖➖➖➖➖➖\n"
    msg += "\nالعب لتزيد من نقاطك وتتصدر القائمة!"
    asyncio.create_task(send_fb_message(user_id, msg))

# ============================================
# 7. محرك الألعاب المتعددة (V10.0)
# ============================================
async def process_matchmaking(game_type):
    queue = WAITING_RPS
    if game_type == 'penalty': queue = WAITING_PENALTIES
    elif game_type == 'split': queue = WAITING_SPLIT
    
    if len(queue) >= 2:
        p1 = queue.pop(0)
        p2 = queue.pop(0)
        
        # نظام مكافحة التحايل (Anti-Farming)
        u1, u2 = sorted([p1, p2])
        today = datetime.now().strftime('%Y-%m-%d')
        res = await db_query("SELECT play_count FROM match_history WHERE user1=? AND user2=? AND date=?", (u1, u2, today), fetchone=True)
        
        if res and res[0] >= 5:
            msg_limit = "🛡️ لقد لعبت كثيراً مع نفس الخصم اليوم (حماية ضد التحايل). تمت إعادتك لطابور البحث للبحث عن خصم جديد..."
            await send_fb_message(p1, msg_limit)
            await send_fb_message(p2, msg_limit)
            queue.append(p1)
            queue.append(p2)
            return

        await start_game(p1, p2, game_type, u1, u2, today)

async def start_game(p1, p2, game_type, u1, u2, today):
    game_id = f"G_{time.time()}_{p1}_{p2}"
    
    p1_name = (await get_user_state(p1))['name'] or "لاعب"
    p2_name = (await get_user_state(p2))['name'] or "لاعب"
    
    ACTIVE_GAMES[game_id] = {
        'p1': p1, 'p2': p2, 'p1_name': p1_name, 'p2_name': p2_name,
        'p1_score': 0, 'p2_score': 0, 'p1_choice': None, 'p2_choice': None,
        'round': 1, 'game_type': game_type
    }
    USER_GAME_MAP[p1] = game_id
    USER_GAME_MAP[p2] = game_id
    
    # التسجيل في السجل
    await db_query("INSERT INTO match_history (user1, user2, date, play_count) VALUES (?, ?, ?, 1) ON CONFLICT(user1, user2, date) DO UPDATE SET play_count = play_count + 1", (u1, u2, today), commit=True)
    
    if game_type == 'rps':
        msg_p1 = f"🔥 بدأت المعركة! (المكافأة: +1 نقطة)\n👤 خصمك هو: {p2_name}\n\n👇 الجولة 1: اختر سلاحك (لديك 20 ثانية):\n1 ⬅️ 🪨 حجر\n2 ⬅️ 📄 ورقة\n3 ⬅️ ✂️ مقص"
        msg_p2 = f"🔥 بدأت المعركة! (المكافأة: +1 نقطة)\n👤 خصمك هو: {p1_name}\n\n👇 الجولة 1: اختر سلاحك (لديك 20 ثانية):\n1 ⬅️ 🪨 حجر\n2 ⬅️ 📄 ورقة\n3 ⬅️ ✂️ مقص"
    elif game_type == 'penalty':
        ACTIVE_GAMES[game_id]['p1_role'] = 'shooter'
        ACTIVE_GAMES[game_id]['p2_role'] = 'goalie'
        msg_p1 = f"⚽ تحدي ضربات الترجيح!\n👤 خصمك: {p2_name}\n\n👇 الجولة 1: أنت **المُسدد**. أين ستسدد الكرة؟ (20 ثانية):\n1 ⬅️ ⬅️ يسار\n2 ⬅️ ⬆️ وسط\n3 ⬅️ ➡️ يمين"
        msg_p2 = f"⚽ تحدي ضربات الترجيح!\n👤 خصمك: {p1_name}\n\n👇 الجولة 1: أنت **الحارس**. أين ستقفز؟ (20 ثانية):\n1 ⬅️ ⬅️ يسار\n2 ⬅️ ⬆️ وسط\n3 ⬅️ ➡️ يمين"
    elif game_type == 'split':
        msg = f"💰 الشكارة والقلب الأسود!\n👤 خصمك هو: {p1_name if p2 else p2_name} (أنت تلعب ضد {p2_name})\n\nهناك 4 نقاط في المنتصف، القرار يعود لكما في الجولة الوحيدة!\n👇 ماذا ستفعل؟ (لديك 20 ثانية):\n1 ⬅️ 🤝 نتعاون ونتقاسم النقاط\n2 ⬅️ 😈 أقوم بخداعه وآخذ كل النقاط"
        msg_p1 = msg.replace(f"ضد {p2_name}", f"ضد {p2_name}")
        msg_p2 = f"💰 الشكارة والقلب الأسود!\n👤 خصمك هو: {p1_name}\n\nهناك 4 نقاط في المنتصف، القرار يعود لكما في الجولة الوحيدة!\n👇 ماذا ستفعل؟ (لديك 20 ثانية):\n1 ⬅️ 🤝 نتعاون ونتقاسم النقاط\n2 ⬅️ 😈 أقوم بخداعه وآخذ كل النقاط"

    asyncio.create_task(send_fb_message(p1, msg_p1))
    asyncio.create_task(send_fb_message(p2, msg_p2))
    asyncio.create_task(round_timer(game_id, 1))

async def round_timer(game_id, round_num):
    await asyncio.sleep(20) 
    game = ACTIVE_GAMES.get(game_id)
    if game and game['round'] == round_num: 
        c1 = game['p1_choice']
        c2 = game['p2_choice']
        
        if not c1 and not c2:
            await send_fb_message(game['p1'], "⏱️ انتهى الوقت ولم يقم أحد بالرد. تم إلغاء المباراة.")
            await send_fb_message(game['p2'], "⏱️ انتهى الوقت ولم يقم أحد بالرد. تم إلغاء المباراة.")
            cleanup_game(game_id)
        elif not c1:
            await send_fb_message(game['p1'], "⏱️ انتهى الوقت! لقد خسرت المباراة بالانسحاب 💀.")
            await send_fb_message(game['p2'], f"⏱️ خصمك ({game['p1_name']}) انسحب. مبروك، لقد فزت 🎉.")
            await end_match(game_id, winner=game['p2'], loser=game['p1'], points=1)
        elif not c2:
            await send_fb_message(game['p2'], "⏱️ انتهى الوقت! لقد خسرت المباراة بالانسحاب 💀.")
            await send_fb_message(game['p1'], f"⏱️ خصمك ({game['p2_name']}) انسحب. مبروك، لقد فزت 🎉.")
            await end_match(game_id, winner=game['p1'], loser=game['p2'], points=1)

async def evaluate_round(game_id):
    game = ACTIVE_GAMES.get(game_id)
    if not game: return
    
    gt = game['game_type']
    p1 = game['p1']; p2 = game['p2']
    c1 = game['p1_choice']; c2 = game['p2_choice']
    n1 = game['p1_name']; n2 = game['p2_name']
    
    if gt == 'rps':
        win_map = {'1': '3', '2': '1', '3': '2'} 
        round_winner = None
        if c1 == c2: res_msg = f"🤝 تعادل! كليكما اختار {CHOICES_RPS[c1]}."
        elif win_map[c1] == c2:
            game['p1_score'] += 1; round_winner = p1
            res_msg = f"💥 ضربة قاضية! {CHOICES_RPS[c1]} يتغلب على {CHOICES_RPS[c2]}."
        else:
            game['p2_score'] += 1; round_winner = p2
            res_msg = f"💥 ضربة قاضية! {CHOICES_RPS[c2]} يتغلب على {CHOICES_RPS[c1]}."
            
        score_str = f"📊 النتيجة: {n1} [{game['p1_score']}] ➖ [{game['p2_score']}] {n2}"
        
        if game['p1_score'] == 2 or game['p2_score'] == 2 or game['round'] == 3:
            if game['p1_score'] > game['p2_score']: final_winner, final_loser = p1, p2
            elif game['p2_score'] > game['p1_score']: final_winner, final_loser = p2, p1
            else: final_winner, final_loser = None, None 
            
            if final_winner:
                w_msg = f"{res_msg}\n{score_str}\n\n🎉 تهانينا! لقد فزت بالمباراة وحصلت على النقاط."
                l_msg = f"{res_msg}\n{score_str}\n\n💀 للأسف خسرت المباراة، حظاً أوفر في المرة القادمة."
                await send_fb_message(final_winner, w_msg)
                await send_fb_message(final_loser, l_msg)
                await end_match(game_id, final_winner, final_loser, points=1)
            else:
                d_msg = f"{res_msg}\n{score_str}\n\n🤝 انتهت المباراة بالتعادل! لم يفز أحد بالنقاط."
                await send_fb_message(p1, d_msg)
                await send_fb_message(p2, d_msg)
                cleanup_game(game_id)
        else:
            game['round'] += 1; game['p1_choice'] = None; game['p2_choice'] = None
            next_msg = f"{res_msg}\n{score_str}\n\n👇 الجولة {game['round']}: اختر سلاحك بسرعة (20 ثانية):\n1 ⬅️ 🪨 حجر\n2 ⬅️ 📄 ورقة\n3 ⬅️ ✂️ مقص"
            await send_fb_message(p1, next_msg); await send_fb_message(p2, next_msg)
            asyncio.create_task(round_timer(game_id, game['round']))
            
    elif gt == 'penalty':
        shooter_id = p1 if game['p1_role'] == 'shooter' else p2
        goalie_id = p2 if game['p2_role'] == 'goalie' else p1
        shooter_choice = c1 if shooter_id == p1 else c2
        goalie_choice = c2 if goalie_id == p2 else c1
        shooter_name = n1 if shooter_id == p1 else n2
        goalie_name = n2 if goalie_id == p2 else n1
        
        if shooter_choice == goalie_choice:
            res_msg = f"🧤 تصدٍ رائع! الحارس {goalie_name} توقع الزاوية ({CHOICES_PEN[goalie_choice]}) وأنقذ المرمى."
            if goalie_id == p1: game['p1_score'] += 1
            else: game['p2_score'] += 1
        else:
            res_msg = f"⚽ هددددف! {shooter_name} سدد في ({CHOICES_PEN[shooter_choice]}) وخدع الحارس."
            if shooter_id == p1: game['p1_score'] += 1
            else: game['p2_score'] += 1

        score_str = f"📊 النتيجة: {n1} [{game['p1_score']}] ➖ [{game['p2_score']}] {n2}"

        if game['round'] == 2:
            if game['p1_score'] > game['p2_score']: final_winner, final_loser = p1, p2
            elif game['p2_score'] > game['p1_score']: final_winner, final_loser = p2, p1
            else: final_winner, final_loser = None, None 
            
            if final_winner:
                w_msg = f"{res_msg}\n{score_str}\n\n🎉 تهانينا! لقد فزت في المباراة وحصلت على النقاط."
                l_msg = f"{res_msg}\n{score_str}\n\n💀 للأسف خسرت المباراة."
                await send_fb_message(final_winner, w_msg); await send_fb_message(final_loser, l_msg)
                await end_match(game_id, final_winner, final_loser, points=1)
            else:
                d_msg = f"{res_msg}\n{score_str}\n\n🤝 انتهت المباراة بالتعادل التام! لم يحصل أحد على نقاط."
                await send_fb_message(p1, d_msg); await send_fb_message(p2, d_msg)
                cleanup_game(game_id)
        else:
            game['round'] += 1; game['p1_choice'] = None; game['p2_choice'] = None
            game['p1_role'] = 'goalie' if game['p1_role'] == 'shooter' else 'shooter'
            game['p2_role'] = 'goalie' if game['p2_role'] == 'shooter' else 'shooter'
            
            msg_p1 = f"{res_msg}\n{score_str}\n\n👇 الجولة {game['round']}: أنت الآن **{'المُسدد' if game['p1_role'] == 'shooter' else 'الحارس'}**. اختر اتجاهك (20 ثانية):\n1 ⬅️ ⬅️ يسار\n2 ⬅️ ⬆️ وسط\n3 ⬅️ ➡️ يمين"
            msg_p2 = f"{res_msg}\n{score_str}\n\n👇 الجولة {game['round']}: أنت الآن **{'المُسدد' if game['p2_role'] == 'shooter' else 'الحارس'}**. اختر اتجاهك (20 ثانية):\n1 ⬅️ ⬅️ يسار\n2 ⬅️ ⬆️ وسط\n3 ⬅️ ➡️ يمين"
            await send_fb_message(p1, msg_p1); await send_fb_message(p2, msg_p2)
            asyncio.create_task(round_timer(game_id, game['round']))
            
    elif gt == 'split':
        if c1 == '1' and c2 == '1':
            res_msg = f"🤝 رجال! كليكما اختار التعاون. تم تقسيم النقاط بسلام (+2 لكل لاعب)."
            await update_user_points(p1, 2); await update_user_points(p2, 2)
        elif c1 == '1' and c2 == '2':
            res_msg = f"😈 صدمة! لقد قام {n2} بخداع {n1} وأخذ كل النقاط!\n({n2} حصل على +4، و {n1} خسر -1)."
            await update_user_points(p1, -1); await update_user_points(p2, 4)
        elif c1 == '2' and c2 == '1':
            res_msg = f"😈 صدمة! لقد قام {n1} بخداع {n2} وأخذ كل النقاط!\n({n1} حصل على +4، و {n2} خسر -1)."
            await update_user_points(p1, 4); await update_user_points(p2, -1)
        elif c1 == '2' and c2 == '2':
            res_msg = f"💔 طمع! كليكما حاول خداع الآخر. ضاعت النقاط وتمت معاقبتكما بخصم (-2 نقاط لكل لاعب)."
            await update_user_points(p1, -2); await update_user_points(p2, -2)
            
        await send_fb_message(p1, res_msg); await send_fb_message(p2, res_msg)
        cleanup_game(game_id)

async def end_match(game_id, winner, loser, points=1):
    await update_user_points(winner, points)
    cleanup_game(game_id)

def cleanup_game(game_id):
    if game_id in ACTIVE_GAMES:
        p1 = ACTIVE_GAMES[game_id]['p1']; p2 = ACTIVE_GAMES[game_id]['p2']
        if p1 in USER_GAME_MAP: del USER_GAME_MAP[p1]
        if p2 in USER_GAME_MAP: del USER_GAME_MAP[p2]
        del ACTIVE_GAMES[game_id]


# ============================================
# 8. محرك جازي الصافي (Asynchronous API)
# ============================================
def extract_arabic_msg(data_json):
    if not data_json: return "العملية غير متوفرة أو رصيدك غير كافٍ."
    try:
        if 'message' in data_json and isinstance(data_json['message'], dict):
            return data_json['message'].get('ar', data_json['message'].get('fr', str(data_json['message'])))
        elif 'message' in data_json: return str(data_json['message'])
        elif 'fault' in data_json and isinstance(data_json['fault'], dict):
            return str(data_json['fault'].get('description', 'خطأ غير معروف'))
        elif 'description' in data_json: return str(data_json['description'])
    except: pass
    return "العملية غير متوفرة أو رصيدك غير كافٍ."

async def request_otp_async(phone):
    try:
        payload = {"consent-agreement": [{"marketing-notifications": False}], "is-consent": True}
        res = await safe_request('POST', f"{BASE_URL}/oauth2/registration", params={'msisdn': phone, 'client_id': CLIENT_ID, 'scope': "smsotp"}, json=payload, headers=HEADERS)
        return res['status'] == 200
    except: return False

async def verify_otp_async(phone, otp):
    h = HEADERS.copy()
    h['Content-Type'] = "application/x-www-form-urlencoded"
    payload = {'otp': str(otp).strip(), 'mobileNumber': str(phone).strip(), 'scope': 'djezzyAppV2', 'client_id': CLIENT_ID, 'client_secret': CLIENT_SECRET, 'grant_type': 'mobile'}
    res = await safe_request('POST', f"{BASE_URL}/oauth2/token", data=payload, headers=h)
    if res['status'] == 200: 
        return res['json']
    return None

async def refresh_token_async(refresh_token):
    h = HEADERS.copy()
    h['Content-Type'] = "application/x-www-form-urlencoded"
    payload = {'scope': 'djezzyAppV2', 'client_id': CLIENT_ID, 'client_secret': CLIENT_SECRET, 'grant_type': 'refresh_token', 'refresh_token': refresh_token}
    res = await safe_request('POST', f"{BASE_URL}/oauth2/token", data=payload, headers=h)
    if res['status'] == 200: 
        return res['json']
    return None

async def get_valid_token_db(phone):
    token_data = await db_query("SELECT access_token, refresh_token FROM tokens WHERE phone = ?", (phone,), fetchone=True)
    if not token_data or not token_data[0]: return None
        
    acc_token, ref_token = token_data
    h = HEADERS.copy()
    h['Authorization'] = f"Bearer {acc_token}"
    res = await safe_request('GET', f"{BASE_URL}/api/v1/subscribers/main-balance/{phone}", headers=h, timeout=8)
    
    if res['status'] == 401 and ref_token:
        new_tokens = await refresh_token_async(ref_token)
        if new_tokens and new_tokens.get("access_token"):
            new_acc = new_tokens['access_token']
            new_ref = new_tokens.get('refresh_token', ref_token)
            await db_query("UPDATE tokens SET access_token = ?, refresh_token = ? WHERE phone = ?", (new_acc, new_ref, phone), commit=True)
            return new_acc
        return None 
    return acc_token 

async def get_balance_async(token, phone):
    h = HEADERS.copy()
    h['Authorization'] = f"Bearer {token}"
    res_data = {"main": "0", "prod": []}
    
    r1 = await safe_request('GET', f"{BASE_URL}/api/v1/subscribers/main-balance/{phone}", headers=h, timeout=10)
    if r1['status'] == 401: return "EXPIRED"
    if r1['status'] == 200 and r1['json']: res_data["main"] = r1['json'].get('data', {}).get('mainBalance', '0')
    
    r2 = await safe_request('GET', f"{BASE_URL}/api/v1/subscribers/connected-products-balances/{phone}", headers=h, timeout=10)
    if r2['status'] == 200 and r2['json']:
        for p in r2['json'].get('data', {}).get('products', []):
            n = p.get('commercialName', {}).get('ar', 'باقة'); ex = p.get('expiryAt', '??')
            for b in p.get('balances', []):
                u = b.get('usageUnit', 'MB'); r = b.get('remaining', 0)
                disp = f"{r/1024:.2f} GB" if u == 'MB' and r > 1024 else f"{r} {u}"
                res_data["prod"].append(f"• {n}: {disp} (ينتهي {ex})")
    return res_data

async def activate_paid_offer_async(token, phone, p_code, o_type):
    h = HEADERS.copy()
    h['Authorization'] = f"Bearer {token}"
    
    if o_type == "shake":
        shake_url = f"{BASE_URL}/api/v1/services/shake/{phone}"
        offer_found = False
        for _ in range(5):
            res_get = await safe_request('GET', shake_url, headers=h, timeout=8)
            if res_get['status'] == 200 and res_get['json']:
                data = res_get['json'].get('data', {})
                if data.get('code') == p_code: offer_found = True; break
            elif res_get['status'] == 401: return {"status": "EXPIRED", "msg": "انتهت صلاحية الجلسة."}
            elif res_get['status'] == 429: await asyncio.sleep(1.5)
            
        if not offer_found: return {"status": "FAILED", "msg": "العرض غير متوفر في حسابك حالياً. يرجى المحاولة لاحقاً."}
        post_url = shake_url
    else:
        post_url = f"{BASE_URL}/api/v1/subscribers/activate-product/{phone}"

    r = await safe_request('POST', post_url, json={"packageCode": p_code}, headers=h, timeout=10)
    if r['status'] == 401: return {"status": "EXPIRED", "msg": "انتهت صلاحية الجلسة."}
    if r['status'] in [200, 201]: return {"status": "SUCCESS", "msg": "تم التفعيل بنجاح."}
    if r['status'] == 500: return {"status": "ERROR", "msg": "تعذر الاتصال بالخادم."}
    return {"status": "FAILED", "msg": extract_arabic_msg(r['json'])}

async def get_subscription_history_async(token, phone):
    h = HEADERS.copy()
    h['Authorization'] = f"Bearer {token}"
    res = await safe_request('GET', f"{BASE_URL}/api/v1/subscribers/subscription-history/{phone}", headers=h, timeout=10)
    if res['status'] == 401: return "EXPIRED"
    if res['status'] == 200 and res['json']:
        data = res['json'].get('data', [])
        history = []
        for item in data[:5]:
            name = item.get('commercialName', {}).get('ar', 'عرض')
            dt = item.get('subscriptionDateTime', '').split('T')[0]
            history.append(f"▪️ {name} ({dt})")
        return history if history else ["لا توجد تفعيلات حديثة."]
    return None

async def toggle_network_service_async(token, phone, service_id, action="ACTIVATE"):
    h = HEADERS.copy()
    h['Authorization'] = f"Bearer {token}"
    payload = {"serviceId": service_id, "action": action}
    res = await safe_request('POST', f"{BASE_URL}/api/v1/services/network-services/{phone}/toggle", json=payload, headers=h, timeout=10)
    if res['status'] == 401: return "EXPIRED"
    if res['status'] in [200, 201]: return "SUCCESS"
    if res['status'] == 500: return "تعذر الاتصال بالشبكة."
    return extract_arabic_msg(res['json'])

async def send_free_sms_async(token, sender, receiver, sms_type):
    url = f"{BASE_URL}/api/v1/customer-care/bip-sms/{sender}"
    h = HEADERS.copy()
    h['Authorization'] = f"Bearer {token}"
    payload = {"msisdnReceiver": int(receiver), "type": "CALLME" if sms_type == "call" else "FLEXYLI"}
    r = await safe_request('POST', url, json=payload, headers=h, timeout=10)
    if r['status'] == 401: return "EXPIRED"
    if r['status'] in [200, 201]: return "SUCCESS"
    if r['status'] == 500: return "تعذر الاتصال بالشبكة."
    return extract_arabic_msg(r['json'])

async def check_free_sms_balance_async(token, phone):
    url = f"{BASE_URL}/api/v1/customer-care/bip-sms/{phone}"
    h = HEADERS.copy()
    h['Authorization'] = f"Bearer {token}"
    r = await safe_request('GET', url, headers=h, timeout=10)
    if r['status'] == 200 and r['json']:
        data = r['json'].get('data', {})
        return {"call": data.get("callMeRemaining", 0), "flexy": data.get("flexyLiRemaining", 0)}
    return None

async def disable_ranati_async(token, phone):
    h = HEADERS.copy()
    h['Authorization'] = f"Bearer {token}"
    url_get = f"{BASE_URL}/content/api/v1/subscribers/{phone}?include=rbt-subscriptions"
    r1 = await safe_request('GET', url_get, headers=h, timeout=10)
    if r1['status'] == 401: return "EXPIRED"
    if r1['status'] == 200 and r1['json']:
        rbt_data = r1['json'].get('data', {}).get('relationships', {}).get('rbt-subscriptions', {}).get('data', [])
        if not rbt_data: return "ALREADY_OFF"
        
        url_del = f"{BASE_URL}/content/api/v1/subscribers/{phone}"
        payload = {"data": {"type": "rbt-subscriptions", "id": str(phone)}}
        r2 = await safe_request('DELETE', url_del, json=payload, headers=h, timeout=10)
        if r2['status'] in [200, 201]: return "SUCCESS"
    return "ERROR"

# ============================================
# 9. إعداد نفق Cloudflare
# ============================================
def start_cloudflared_token():
    binary_path = "./cloudflared"
    if not os.path.exists(binary_path):
        arch = platform.machine().lower()
        if arch in ['aarch64', 'arm64']: url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64"
        elif arch in ['arm', 'armv7l', 'armv8l']: url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm"
        else: url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64"
        try:
            urllib.request.urlretrieve(url, binary_path)
            os.chmod(binary_path, 0o755)
        except Exception: return
    print("\n" + "🔥"*20 + "\n🚀 جاري الاتصال بنفق Cloudflare...\n" + "🔥"*20 + "\n", flush=True)
    try: subprocess.Popen([binary_path, "tunnel", "--no-autoupdate", "run", "--token", CF_TOKEN], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except: pass

async def process_admin_echo(target_id, cmd):
    if cmd.startswith("/ban"):
        m = int(cmd.split()[1]) if len(cmd.split()) > 1 else 60
        await update_user_state(target_id, banned_until=time.time() + (m * 60), state='idle')
        await send_fb_message(target_id, f"⛔ تم حظرك لمدة {m} دقيقة.")
    elif cmd.startswith("/unban"):
        await update_user_state(target_id, banned_until=0)
        await send_fb_message(target_id, "✅ تم رفع الإيقاف عنك.")

# ============================================
# 10. مهام الخلفية 
# ============================================
async def background_tasks():
    while True:
        try:
            now = time.time()
            rows = await db_query("SELECT id, send_time, message FROM scheduled_broadcasts WHERE status = 'pending' AND send_time <= ?", (now,), fetchall=True)
            if rows:
                for rid, send_time, msg in rows:
                    all_users = await db_query("SELECT user_id FROM users", fetchall=True)
                    for (uid,) in all_users:
                        if uid != ADMIN_PSID: asyncio.create_task(send_fb_message(uid, f"📢 إشعار إداري:\n\n{msg}"))
                    await db_query("UPDATE scheduled_broadcasts SET status = 'completed' WHERE id = ?", (rid,), commit=True)
        except: pass
        await asyncio.sleep(60)

# ============================================
# 11. أوامر المطور 
# ============================================
async def handle_admin_command(user_id, text):
    if text == "/help":
        help_msg = (
            "👑 أوامر المطور:\n"
            "/stats - إحصائيات عامة\n"
            "/broadcast <msg> - إذاعة فورية\n"
            "/schedule_broadcast YYYY-MM-DD HH:MM:SS <msg> - إذاعة مجدولة\n"
            "/maintenance on [msg] - تفعيل الصيانة\n"
            "/maintenance off - إيقاف الصيانة\n"
            "/ban <minutes> - حظر مستخدم\n"
            "/unban - رفع الحظر"
        )
        asyncio.create_task(send_fb_message(user_id, help_msg))
        return True
        
    elif text == "/stats":
        tot_row = await db_query("SELECT COUNT(*) FROM users", fetchone=True)
        tot_users = tot_row[0] if tot_row else 0
        stats_data = await db_query("SELECT key, value FROM settings WHERE key LIKE 'stat_%'", fetchall=True)
        s_dict = {k: int(v) for k, v in stats_data} if stats_data else {}
        msg = f"📊 إحصائيات البوت الشاملة:\n" + "➖"*15 + "\n"
        msg += f"👥 إجمالي المستخدمين: {tot_users}\n"
        msg += f"🏃 تفعيلات 2 جيجابايت: {s_dict.get('stat_walk_2gb', 0)}\n"
        msg += f"💬 رسائل مجانية مرسلة: {s_dict.get('stat_free_sms', 0)}\n\n"
        msg += f"🛒 تفعيلات العروض المدفوعة:\n"
        offer_found = False
        for o_key, o_data in CATALOG.items():
            cnt = s_dict.get(f'stat_offer_{o_key}', 0)
            if cnt > 0:
                msg += f"▪️ {o_data['data']} ({o_data['price']} د.ج): {cnt} مرة\n"
                offer_found = True
        if not offer_found: msg += "▪️ لم يتم تفعيل أي عرض مدفوع بعد.\n"
        asyncio.create_task(send_fb_message(user_id, msg))
        return True
            
    elif text.startswith("/broadcast "):
        msg_to_send = text.replace("/broadcast ", "", 1).strip()
        if msg_to_send: asyncio.create_task(run_broadcast(user_id, msg_to_send))
        return True
        
    elif text.startswith("/schedule_broadcast "):
        parts = text.split(maxsplit=3)
        if len(parts) < 4:
            asyncio.create_task(send_fb_message(user_id, "⚠️ الاستخدام الصحيح: /schedule_broadcast YYYY-MM-DD HH:MM:SS نص الرسالة"))
            return True
        date_str = parts[1]; time_str = parts[2]; message = parts[3]
        try:
            dt = datetime.strptime(f"{date_str} {time_str}", "%Y-%m-%d %H:%M:%S")
            send_time = dt.timestamp()
            await db_query("INSERT INTO scheduled_broadcasts (send_time, message, status) VALUES (?, ?, 'pending')", (send_time, message), commit=True)
            asyncio.create_task(send_fb_message(user_id, f"✅ تمت جدولة الرسالة بنجاح للوقت {date_str} {time_str}."))
        except:
            asyncio.create_task(send_fb_message(user_id, "⚠️ تنسيق الوقت غير صحيح. يرجى التقيد بالصيغة: YYYY-MM-DD HH:MM:SS"))
        return True
        
    elif text.startswith("/maintenance on"):
        parts = text.split(maxsplit=1)
        msg = "عذراً، البوت تحت الصيانة حالياً. سنعود قريباً!" if len(parts) == 1 else parts[1]
        await db_query("UPDATE settings SET value = '1' WHERE key = 'maintenance'", commit=True)
        await db_query("UPDATE settings SET value = ? WHERE key = 'maintenance_msg'", (msg,), commit=True)
        asyncio.create_task(send_fb_message(user_id, "✅ تم تفعيل وضع الصيانة."))
        return True
        
    elif text == "/maintenance off":
        await db_query("UPDATE settings SET value = '0' WHERE key = 'maintenance'", commit=True)
        asyncio.create_task(send_fb_message(user_id, "✅ تم إيقاف وضع الصيانة."))
        return True
        
    return False

# ============================================
# 12. العقل المدبر Async 
# ============================================
async def process_message(user_id, event):
    user_data = await get_user_state(user_id)
    current_time = time.time()
    
    raw_text = event.get('message', {}).get('text', '').strip()
    text_no_spaces = raw_text.replace(" ", "")
    has_attachment = 'attachments' in event.get('message', {})
    
    is_like = False
    if has_attachment:
        for att in event['message']['attachments']:
            if att.get('type') == 'image' and 'sticker_id' in att.get('payload', {}):
                sticker_id = str(att['payload']['sticker_id'])
                if sticker_id in ['369239263222822', '369239383222810', '369239343222814']: is_like = True
    
    # فلترة اللعب قبل أي شيء
    if user_id in USER_GAME_MAP:
        if is_like or has_attachment: return
        game_id = USER_GAME_MAP[user_id]
        game = ACTIVE_GAMES.get(game_id)
        if game:
            valid_choices = ['1', '2'] if game['game_type'] == 'split' else ['1', '2', '3']
            if text_no_spaces in valid_choices:
                if game['p1'] == user_id and not game['p1_choice']: game['p1_choice'] = text_no_spaces
                elif game['p2'] == user_id and not game['p2_choice']: game['p2_choice'] = text_no_spaces
                else: return
                
                await send_fb_message(user_id, "✅ تم تسجيل اختيارك... ننتظر الخصم.")
                
                if game['p1_choice'] and game['p2_choice']:
                    await evaluate_round(game_id)
            else:
                await send_fb_message(user_id, f"⚠️ يرجى اختيار أحد الأرقام المتاحة فقط.")
        return 

    if text_no_spaces == "👍" or is_like:
        asyncio.create_task(send_fb_message(user_id, "شكراً لتفاعلك! يمكنك دائماً كتابة رقمك للمتابعة، أو إرسال (0) للعودة للقائمة الرئيسية.\n\n👍 لا تنسَ متابعة صفحتنا الرسمية [ boykta net ¹ ]"))
        return

    # ==========================================
    # نظام التسجيل (الاسم + الرابط + الرقم) 
    # ==========================================
    if not user_data['name'] or user_data['name'].strip() == "":
        if user_data['state'] != "waiting_for_name":
            msg = "مرحباً بك في بوت Boykta! 🚀 أقوى بوت لخدمات جازي.\n\nلقد أضفنا ألعاباً وتحديات جديدة! للبدء، يرجى كتابة اسمك أو لقبك:"
            asyncio.create_task(send_typing_indicator(user_id))
            asyncio.create_task(send_fb_message(user_id, msg))
            await update_user_state(user_id, state="waiting_for_name")
            return

    if user_data['state'] == "waiting_for_name":
        if re.search(r'\d', raw_text):
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، يرجى كتابة اسم حقيقي بالحروف (بدون أرقام)."))
            return
        if len(raw_text) > 30:
            asyncio.create_task(send_fb_message(user_id, "⚠️ الاسم الذي أدخلته طويل جداً. يرجى اختصاره."))
            return
        
        await update_user_state(user_id, name=raw_text, state="waiting_for_link")
        msg = (f"تشرفنا بك يا {raw_text}! 🤍 تم حفظ اسمك بنجاح.\n\n"
               "🔗 يرجى إرسال رابط حسابك الشخصي (ليظهر في قائمة أفضل اللاعبين وتزيد المنافسة).\n(أرسل '0' لتخطي هذه الخطوة):")
        asyncio.create_task(send_fb_message(user_id, msg))
        return
        
    if user_data['state'] == "waiting_for_link":
        link_val = "" if text_no_spaces == "0" else raw_text
        await db_query("UPDATE users SET link = ? WHERE user_id = ?", (link_val, str(user_id)), commit=True)
        await update_user_state(user_id, state="waiting_for_phone")
        msg = "✅ تم حفظ التفضيلات.\n\n📱 أخيراً، يرجى إرسال رقم جازي الخاص بك (مثال: 0770000000):"
        asyncio.create_task(send_fb_message(user_id, msg))
        return

    if user_data['state'] == "editing_name":
        if re.search(r'\d', raw_text):
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، يرجى كتابة اسم حقيقي بالحروف (بدون أرقام)."))
            return
        if len(raw_text) > 30:
            asyncio.create_task(send_fb_message(user_id, "⚠️ الاسم الذي أدخلته طويل جداً. يرجى اختصاره."))
            return
        await update_user_state(user_id, name=raw_text, state="editing_link")
        asyncio.create_task(send_fb_message(user_id, f"✅ تم تحديث اسمك إلى: {raw_text}\n\n🔗 يرجى إرسال رابط حسابك الجديد (أو أرسل '0' لإزالته، أو '1' للإبقاء على الرابط القديم):"))
        return
        
    if user_data['state'] == "editing_link":
        if text_no_spaces == "0": await db_query("UPDATE users SET link = '' WHERE user_id = ?", (str(user_id),), commit=True)
        elif text_no_spaces != "1": await db_query("UPDATE users SET link = ? WHERE user_id = ?", (raw_text, str(user_id)), commit=True)
        
        await update_user_state(user_id, state="idle")
        updated_data = await get_user_state(user_id)
        asyncio.create_task(send_fb_message(user_id, "✅ تم تحديث ملفك الشخصي بنجاح."))
        await send_main_menu(user_id, updated_data['phone'], updated_data['name'], updated_data['points'])
        return

    if user_data['state'] == "waiting_for_phone":
        phone_match_onboarding = re.search(r"^(?:\+213|213|0)(7\d{8})$", text_no_spaces)
        if phone_match_onboarding:
            new_phone = "213" + phone_match_onboarding.group(1)
            await update_user_state(user_id, phone=new_phone, state="idle", is_new_user=0)
            
            success_msg = f"✅ تم إعداد حسابك بنجاح يا {user_data['name']}!\n🎮 يمكنك الاستمتاع بالخدمات والألعاب الآن."
            asyncio.create_task(send_fb_message(user_id, success_msg))
            await send_main_menu(user_id, new_phone, user_data['name'], user_data['points'])
        else:
            asyncio.create_task(send_fb_message(user_id, "⚠️ رقم خاطئ! يرجى إدخال رقم جازي صحيح (مثال: 0770000000)."))
        return

    if str(user_id) == ADMIN_PSID and raw_text.startswith("/"):
        if await handle_admin_command(user_id, raw_text): return

    payload = ""
    t = text_no_spaces.lower()
    if t in ["0", "رجوع", "الغاء", "إلغاء"]: payload = "MAIN_MENU"
    
    if payload == "MAIN_MENU":
        if user_id in WAITING_RPS: WAITING_RPS.remove(user_id)
        if user_id in WAITING_PENALTIES: WAITING_PENALTIES.remove(user_id)
        if user_id in WAITING_SPLIT: WAITING_SPLIT.remove(user_id)

    phone_match = re.search(r"^(?:\+213|213|0)(7\d{8})$", text_no_spaces)
    if phone_match and user_data['state'] == "idle":
        new_phone = "213" + phone_match.group(1)
        await update_user_state(user_id, phone=new_phone)
        
        is_verified = await db_query("SELECT 1 FROM verified_numbers WHERE user_id = ? AND phone = ?", (str(user_id), new_phone), fetchone=True)
        token = await get_valid_token_db(new_phone)
        
        if is_verified and token:
            asyncio.create_task(send_fb_message(user_id, f"✅ تم تسجيل الدخول مباشرة للرقم {get_masked_phone(new_phone)}."))
            await send_main_menu(user_id, new_phone, user_data['name'], user_data['points'])
            return
            
        if current_time - user_data['last_otp_time'] < 300:
            asyncio.create_task(send_fb_message(user_id, f"⏳ يرجى الانتظار يا {user_data['name']}، يمكنك طلب رمز جديد بعد {format_time(300 - (current_time - user_data['last_otp_time']))}."))
            return
            
        await update_user_state(user_id, last_action_time=current_time)
        asyncio.create_task(send_typing_indicator(user_id))
        asyncio.create_task(send_fb_message(user_id, f"⏳ جاري إرسال رمز التحقق إلى الرقم {get_masked_phone(new_phone)}، يرجى الانتظار..."))
        
        if await request_otp_async(new_phone):
            await update_user_state(user_id, last_otp_time=current_time, otp_request_time=current_time, state="waiting_for_otp")
            asyncio.create_task(send_fb_message(user_id, f"✅ تم إرسال الرمز.\n👇 يرجى إدخال الرمز المكون من 6 أرقام فور وصوله:\n(أو أرسل '0' للعودة)"))
        else: asyncio.create_task(send_fb_message(user_id, "❌ عذراً، واجهنا مشكلة في الاتصال بالخادم. يرجى المحاولة لاحقاً."))
        return

    if not payload and not has_attachment and not is_like:
        if user_data['state'] == "idle" and user_data['phone']:
            if t == "1": payload = "ACT_2GB"
            elif t == "2": payload = "MENU_FREE_SMS"
            elif t == "3": payload = "MENU_OFFERS"
            elif t == "4": payload = "BAL"
            elif t == "5": payload = "MENU_NETWORK"
            elif t == "6": payload = "MENU_GAME"
            elif t == "7": payload = "EDIT_PROFILE"
            elif t == "8": payload = "DISABLE_RANATI"
            
        elif user_data['state'] == "menu_free_sms":
            if t == "1": payload = "REQ_CALL_ME"
            elif t == "2": payload = "REQ_FLEXY_ME"
            elif t == "3": payload = "CHECK_FREE_SMS_BAL"
            
        elif user_data['state'] == "menu_offers":
            try:
                idx = int(t) - 1
                keys = list(CATALOG.keys())
                if 0 <= idx < len(keys): payload = f"CONFIRM_OFFER_{keys[idx]}"
            except: pass
            
        elif user_data['state'] == "menu_network":
            if t == "1": payload = "NET_MASK_ON"
            elif t == "2": payload = "NET_MASK_OFF"
            elif t == "3": payload = "NET_WAIT"
            elif t == "4": payload = "SUB_HISTORY"
            
        elif user_data['state'] == "confirm_offer":
            if t in ["1", "نعم", "تأكيد"]: payload = "EXECUTE_OFFER"
            
        elif user_data['state'] == "menu_game":
            if t == "1": payload = "JOIN_GAME_RPS"
            elif t == "2": payload = "JOIN_GAME_PENALTY"
            elif t == "3": payload = "JOIN_GAME_SPLIT"
            elif t == "4": payload = "LEADERBOARD"

    if str(user_id) != ADMIN_PSID:
        m_mode_row = await db_query("SELECT value FROM settings WHERE key = 'maintenance'", fetchone=True)
        m_mode = m_mode_row[0] if m_mode_row else '0'
        if m_mode == '1':
            m_msg_row = await db_query("SELECT value FROM settings WHERE key = 'maintenance_msg'", fetchone=True)
            m_msg = m_msg_row[0] if m_msg_row else 'عذراً، البوت تحت الصيانة حالياً. سنعود قريباً!'
            asyncio.create_task(send_fb_message(user_id, m_msg))
            return
        if user_data['banned_until'] > current_time:
            asyncio.create_task(send_fb_message(user_id, f"⛔ عذراً، حسابك محظور مؤقتاً."))
            return

    heavy_actions = ["BAL", "ACT_2GB", "EXECUTE_OFFER"]
    if payload in heavy_actions and str(user_id) != ADMIN_PSID:
        last_heavy = user_data.get('last_action_time', 0)
        if current_time - last_heavy < PROXY_COOLDOWN:
            rem = PROXY_COOLDOWN - (current_time - last_heavy)
            asyncio.create_task(send_fb_message(user_id, f"🛡️ يرجى الانتظار! يمكنك تنفيذ عملية جديدة بعد {format_time(rem)}."))
            return
    
    if payload == "MAIN_MENU":
        await update_user_state(user_id, state="idle")
        asyncio.create_task(send_fb_message(user_id, "✅ تمت العودة."))
        if user_data['phone']: await send_main_menu(user_id, user_data['phone'], user_data['name'], user_data['points'])
        else: asyncio.create_task(send_fb_message(user_id, "يرجى إرسال رقم جازي الخاص بك للبدء."))
        return

    if payload == "MENU_FREE_SMS":
        await update_user_state(user_id, state="menu_free_sms")
        await send_free_sms_menu(user_id)
        return
    if payload == "MENU_OFFERS":
        await update_user_state(user_id, state="menu_offers")
        await send_offers_menu(user_id)
        return
    if payload == "MENU_NETWORK":
        await update_user_state(user_id, state="menu_network")
        await send_network_menu(user_id)
        return
    if payload == "MENU_GAME":
        await update_user_state(user_id, state="menu_game")
        await send_game_menu(user_id, user_data['points'])
        return
    if payload == "LEADERBOARD":
        await show_leaderboard(user_id)
        return
    if payload == "EDIT_PROFILE":
        await update_user_state(user_id, state="editing_name")
        asyncio.create_task(send_fb_message(user_id, f"👤 إعدادات الملف الشخصي:\n\nاسمك الحالي هو: {user_data['name']}\nيرجى إرسال اسمك الجديد لتحديثه:"))
        return

    # الانضمام لغرف اللعب
    if payload == "JOIN_GAME_RPS":
        if user_id in WAITING_RPS: return
        WAITING_RPS.append(user_id)
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري البحث عن خصم في لعبة (حجر، ورقة، مقص)... (أرسل 0 للإلغاء)"))
        asyncio.create_task(process_matchmaking('rps'))
        return
    if payload == "JOIN_GAME_PENALTY":
        if user_id in WAITING_PENALTIES: return
        WAITING_PENALTIES.append(user_id)
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري البحث عن خصم في تحدي (ضربات الترجيح)... (أرسل 0 للإلغاء)"))
        asyncio.create_task(process_matchmaking('penalty'))
        return
    if payload == "JOIN_GAME_SPLIT":
        if user_id in WAITING_SPLIT: return
        WAITING_SPLIT.append(user_id)
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري البحث عن خصم في تحدي (التعاون أو الخداع)... (أرسل 0 للإلغاء)"))
        asyncio.create_task(process_matchmaking('split'))
        return
    
    if payload == "DISABLE_RANATI":
        target_phone = user_data['phone']
        if not target_phone: 
            asyncio.create_task(send_fb_message(user_id, "يرجى كتابة رقمك أولاً."))
            return
        token = await get_valid_token_db(target_phone)
        if not token:
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، انتهت صلاحية جلستك الحالية. يرجى إعادة إرسال رقمك لتوثيق الدخول من جديد."))
            await update_user_state(user_id, state="idle"); return
        asyncio.create_task(send_typing_indicator(user_id))
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري فحص خدمة 'رناتي' في حسابك..."))
        res = await disable_ranati_async(token, target_phone)
        if res == "SUCCESS": asyncio.create_task(send_fb_message(user_id, "✅ تم العثور على خدمة 'رناتي' مفعلة وتم إلغاؤها بنجاح لحماية رصيدك."))
        elif res == "ALREADY_OFF": asyncio.create_task(send_fb_message(user_id, "🛡️ الخدمة غير مفعلة في حسابك، رصيدك في أمان."))
        elif res == "EXPIRED": asyncio.create_task(send_fb_message(user_id, "❌ انتهت صلاحية الجلسة."))
        else: asyncio.create_task(send_fb_message(user_id, "❌ تعذر الاتصال بخوادم الشبكة. يرجى المحاولة لاحقاً."))
        return

    if payload == "BAL":
        target_phone = user_data['phone']
        if not target_phone: 
            asyncio.create_task(send_fb_message(user_id, "يرجى كتابة رقمك أولاً."))
            return
        asyncio.create_task(send_typing_indicator(user_id))
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري جلب تفاصيل الرصيد، يرجى الانتظار..."))
        token = await get_valid_token_db(target_phone)
        if not token:
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، انتهت صلاحية جلستك الحالية. يرجى إعادة إرسال رقمك لتوثيق الدخول من جديد."))
            await update_user_state(user_id, state="idle"); return
        res = await get_balance_async(token, target_phone)
        if isinstance(res, dict):
            await update_user_state(user_id, last_action_time=current_time)
            txt = f"💰 الرصيد الرئيسي: {res['main']} د.ج\n\n📦 الباقات النشطة:\n" + ("\n".join(res['prod']) if res['prod'] else "لا توجد أي باقات إنترنت نشطة.")
            asyncio.create_task(send_fb_message(user_id, txt))
        else: asyncio.create_task(send_fb_message(user_id, "❌ تعذر جلب تفاصيل الرصيد، يرجى المحاولة لاحقاً."))
        return
    
    if payload in ["NET_MASK_ON", "NET_MASK_OFF", "NET_WAIT"]:
        my_phone = user_data['phone']
        token = await get_valid_token_db(my_phone)
        if not token: 
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، انتهت صلاحية جلستك الحالية. يرجى إعادة إرسال رقمك لتوثيق الدخول من جديد."))
            await update_user_state(user_id, state="idle"); return
        service_id = "APPELMASQUE" if payload != "NET_WAIT" else "CALLWAIT"
        action = "DEACTIVATE" if payload == "NET_MASK_OFF" else "ACTIVATE"
        asyncio.create_task(send_typing_indicator(user_id))
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري تنفيذ الطلب..."))
        res = await toggle_network_service_async(token, my_phone, service_id, action)
        if res == "SUCCESS": asyncio.create_task(send_fb_message(user_id, "✅ تمت العملية بنجاح!"))
        elif res == "EXPIRED": asyncio.create_task(send_fb_message(user_id, "❌ انتهت صلاحية الجلسة."))
        else: asyncio.create_task(send_fb_message(user_id, f"⚠️ رد الشبكة:\n{res}"))
        return
    
    if payload == "SUB_HISTORY":
        my_phone = user_data['phone']
        token = await get_valid_token_db(my_phone)
        if not token: 
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، انتهت صلاحية جلستك الحالية. يرجى إعادة إرسال رقمك لتوثيق الدخول من جديد."))
            await update_user_state(user_id, state="idle"); return
        asyncio.create_task(send_typing_indicator(user_id))
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري جلب سجل التفعيلات..."))
        history = await get_subscription_history_async(token, my_phone)
        if history == "EXPIRED": asyncio.create_task(send_fb_message(user_id, "❌ انتهت صلاحية الجلسة."))
        elif history: asyncio.create_task(send_fb_message(user_id, "⏱️ سجل التفعيلات الأخيرة الخاصة بك:\n\n" + "\n".join(history)))
        else: asyncio.create_task(send_fb_message(user_id, "❌ تعذر الاتصال بالخادم."))
        return
    
    if payload in ["REQ_CALL_ME", "REQ_FLEXY_ME"]:
        st = "waiting_call_me_phone" if payload == "REQ_CALL_ME" else "waiting_flexy_me_phone"
        await update_user_state(user_id, state=st)
        asyncio.create_task(send_fb_message(user_id, "📱 يرجى إدخال رقم جازي الخاص بالمستلم:\n\n0 ⬅️ للعودة"))
        return
        
    if user_data['state'] in ["waiting_call_me_phone", "waiting_flexy_me_phone"]:
        m = re.search(r"^(?:\+213|213|0)(7\d{8})$", text_no_spaces)
        if not m: 
            asyncio.create_task(send_fb_message(user_id, "❌ رقم خاطئ. يرجى إدخال رقم جازي صحيح."))
            return
        target = "213" + m.group(1)
        asyncio.create_task(send_typing_indicator(user_id))
        token = await get_valid_token_db(user_data['phone'])
        if not token: 
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، انتهت صلاحية جلستك الحالية. يرجى إعادة إرسال رقمك لتوثيق الدخول من جديد."))
            await update_user_state(user_id, state="idle"); return
        sms_type = "call" if user_data['state'] == "waiting_call_me_phone" else "flexy"
        res = await send_free_sms_async(token, user_data['phone'], target, sms_type)
        if res == "SUCCESS": asyncio.create_task(send_fb_message(user_id, "✅ تم إرسال الرسالة بنجاح!"))
        elif res == "EXPIRED": asyncio.create_task(send_fb_message(user_id, "❌ انتهت صلاحية الجلسة."))
        else: asyncio.create_task(send_fb_message(user_id, f"⚠️ رد الشبكة:\n{res}"))
        await update_user_state(user_id, state="idle")
        await asyncio.sleep(1)
        await send_main_menu(user_id, user_data['phone'], user_data['name'], user_data['points'])
        return
        
    if payload == "CHECK_FREE_SMS_BAL":
        token = await get_valid_token_db(user_data['phone'])
        if not token: 
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، انتهت صلاحية جلستك الحالية. يرجى إعادة إرسال رقمك لتوثيق الدخول من جديد."))
            await update_user_state(user_id, state="idle"); return
        asyncio.create_task(send_typing_indicator(user_id))
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري فحص الرصيد المتبقي..."))
        bal = await check_free_sms_balance_async(token, user_data['phone'])
        if bal is not None:
            msg = f"📊 رصيد الرسائل المجانية المتبقي لليوم:\n📞 كلمني: {bal['call']} رسائل\n💸 فليكسيلي: {bal['flexy']} رسائل"
            asyncio.create_task(send_fb_message(user_id, msg))
        else: asyncio.create_task(send_fb_message(user_id, "❌ تعذر فحص الرصيد."))
        return
    
    if payload.startswith("CONFIRM_OFFER_"):
        offer_key = payload.replace("CONFIRM_OFFER_", "")
        if offer_key not in CATALOG: return
        offer = CATALOG[offer_key]
        await update_user_state(user_id, state="confirm_offer", pending_offer_code=offer_key)
        msg = f"🛡️ تأكيد العملية\nهل أنت متأكد من رغبتك في تفعيل عرض {offer['data']} بقيمة {offer['price']} د.ج؟\n\n1 ⬅️ نعم، تأكيد\n0 ⬅️ لا، إلغاء"
        asyncio.create_task(send_fb_message(user_id, msg))
        return
    
    if payload == "EXECUTE_OFFER" and user_data['state'] == "confirm_offer":
        offer_key = user_data.get('pending_offer_code')
        if not offer_key or offer_key not in CATALOG: await update_user_state(user_id, state="idle"); return
        offer = CATALOG[offer_key]
        my_phone = user_data['phone']
        token = await get_valid_token_db(my_phone)
        if not token: 
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، انتهت صلاحية جلستك الحالية. يرجى إعادة إرسال رقمك لتوثيق الدخول من جديد."))
            await update_user_state(user_id, state="idle"); return
        asyncio.create_task(send_typing_indicator(user_id))
        asyncio.create_task(send_fb_message(user_id, f"⏳ جاري تفعيل العرض، يرجى الانتظار..."))
        res = await activate_paid_offer_async(token, my_phone, offer['code'], offer['type'])
        if res['status'] == "SUCCESS":
            await update_user_state(user_id, last_action_time=current_time)
            await db_query("UPDATE settings SET value = value + 1 WHERE key = ?", (f'stat_offer_{offer_key}',), commit=True)
            asyncio.create_task(send_fb_message(user_id, "✅ تمت العملية بنجاح! تم تفعيل العرض.\n\n👍 لا تنسَ متابعة صفحتنا الرسمية [ boykta net ¹ ] ❤️"))
        else: asyncio.create_task(send_fb_message(user_id, f"❌ عذراً، تعذر تفعيل العرض بسبب:\n{res['msg']}"))
        await update_user_state(user_id, state="idle")
        await asyncio.sleep(1)
        await send_main_menu(user_id, my_phone, user_data['name'], user_data['points'])
        return
    
    if payload == "ACT_2GB":
        target_phone = user_data['phone']
        db_res = await db_query("SELECT last_2gb_activation FROM tokens WHERE phone=?", (target_phone,), fetchone=True)
        if db_res and (current_time - db_res[0] < 604800):
            asyncio.create_task(send_fb_message(user_id, f"⏳ عذراً يا {user_data['name']}! يمكنك تفعيل باقة 2 جيجابايت مجدداً بعد {format_time(604800 - (current_time - db_res[0]))}."))
            return
        token = await get_valid_token_db(target_phone)
        if not token: 
            asyncio.create_task(send_fb_message(user_id, "⚠️ عذراً، انتهت صلاحية جلستك الحالية. يرجى إعادة إرسال رقمك لتوثيق الدخول من جديد."))
            await update_user_state(user_id, state="idle"); return
        asyncio.create_task(send_typing_indicator(user_id))
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري إرسال طلب تفعيل 2 جيجابايت (امشِ واربح)..."))
        h = HEADERS.copy(); h['Authorization'] = f"Bearer {token}"
        try: await safe_request('GET', f"{BASE_URL}/api/v1/services/walk/campaign/{target_phone}", headers=h, timeout=8)
        except: pass
        r = await safe_request('POST', f"{BASE_URL}/api/v1/services/walk/activate-reward/{target_phone}", json={"packageCode": "GIFTWALKWIN2GO"}, headers=h, timeout=8)
        if r['status'] in [200, 201]:
            await update_user_state(user_id, last_action_time=current_time) 
            await db_query("UPDATE tokens SET last_2gb_activation = ? WHERE phone = ?", (current_time, target_phone), commit=True)
            await db_query("UPDATE settings SET value = value + 1 WHERE key = 'stat_walk_2gb'", commit=True) 
            asyncio.create_task(send_fb_message(user_id, "✅ تمت العملية بنجاح! تم تفعيل باقة 2 جيجابايت في حسابك.\n\n👍 لا تنسَ متابعة صفحتنا الرسمية [ boykta net ¹ ] ❤️"))
        else: 
            api_msg = extract_arabic_msg(r['json'])
            asyncio.create_task(send_fb_message(user_id, f"⚠️ تم رفض الطلب من قبل الشبكة.\nالسبب: {api_msg}\n\n(القانون الجديد يتطلب تعبئة رصيد بقيمة 100 د.ج خلال الشهر للحصول على المكافأة)."))
        return

    if user_data['state'] == "waiting_for_otp":
        if current_time - user_data['otp_request_time'] > 240:
            await update_user_state(user_id, state="idle")
            asyncio.create_task(send_fb_message(user_id, "⏰ انتهت صلاحية الرمز. يرجى إرسال رقمك مجدداً لطلب رمز جديد."))
            return
        if not re.match(r"^\d{6}$", text_no_spaces): 
            asyncio.create_task(send_fb_message(user_id, "⚠️ يجب أن يتكون الرمز من 6 أرقام فقط. يرجى التحقق وإعادة الإدخال."))
            return
        asyncio.create_task(send_typing_indicator(user_id))
        asyncio.create_task(send_fb_message(user_id, "⏳ جاري توثيق الحساب، يرجى الانتظار..."))
        target_phone = user_data['phone']
        tokens = await verify_otp_async(target_phone, text_no_spaces)
        if not tokens: 
            asyncio.create_task(send_fb_message(user_id, "❌ الرمز غير صحيح أو منتهي الصلاحية. يرجى المحاولة مرة أخرى."))
            return
        await db_query("INSERT OR IGNORE INTO verified_numbers (user_id, phone) VALUES (?, ?)", (str(user_id), target_phone), commit=True)
        await db_query("""INSERT OR REPLACE INTO tokens 
                    (phone, owner_id, access_token, refresh_token, last_1gb_activation, last_2gb_activation, last_received_invite) 
                    VALUES (?, ?, ?, ?, 
                    COALESCE((SELECT last_1gb_activation FROM tokens WHERE phone=?), 0), 
                    COALESCE((SELECT last_2gb_activation FROM tokens WHERE phone=?), 0),
                    COALESCE((SELECT last_received_invite FROM tokens WHERE phone=?), 0))""", 
                 (target_phone, str(user_id), tokens['access_token'], tokens.get('refresh_token'), target_phone, target_phone, target_phone), commit=True)
        asyncio.create_task(send_fb_message(user_id, f"✅ تم تسجيل الدخول بنجاح للرقم {get_masked_phone(target_phone)}."))
        await update_user_state(user_id, state="idle", otp_request_time=0)
        await send_main_menu(user_id, target_phone, user_data['name'], user_data['points'])
        return

    if not payload and not has_attachment and not is_like:
        if user_data['state'] == "idle":
            if user_data['phone']: await send_main_menu(user_id, user_data['phone'], user_data['name'], user_data['points'])
            else: asyncio.create_task(send_fb_message(user_id, "يرجى إرسال رقم جازي الخاص بك للبدء."))
        else: asyncio.create_task(send_fb_message(user_id, "عفواً، لم يتم التعرف على الأمر. أرسل '0' للعودة للقائمة الرئيسية."))

# ============================================
# 13. نظام العمال الموازي (Task Queue)
# ============================================
async def worker_task():
    while True:
        sender_id, event = await TASK_QUEUE.get()
        if sender_id in PROCESSING_USERS:
            TASK_QUEUE.task_done()
            continue
        PROCESSING_USERS.add(sender_id)
        try: await process_message(sender_id, event)
        except Exception as e: print(f"❌ Worker Error processing msg: {e}", flush=True)
        finally:
            PROCESSING_USERS.discard(sender_id)
            TASK_QUEUE.task_done()

async def start_workers(app):
    for _ in range(20): asyncio.create_task(worker_task())

# ============================================
# 14. إعداد السيرفر Aiohttp
# ============================================
routes = web.RouteTableDef()

@routes.get('/')
async def hello(request): return web.Response(text="<h1>✅ Boykta FB Async Server V10.0 (Gaming Edition) is Live!</h1>", content_type='text/html')

@routes.get('/webhook')
async def verify_webhook(request):
    if request.query.get("hub.verify_token") == FB_VERIFY_TOKEN: return web.Response(text=request.query.get("hub.challenge"))
    return web.Response(text="Failed", status=403)

@routes.post('/webhook')
async def handle_webhook(request):
    try:
        data = await request.json()
        if data['object'] == 'page':
            for entry in data['entry']:
                for event in entry['messaging']:
                    sender_id = event.get('sender', {}).get('id')
                    ts = event.get('timestamp', 0) / 1000.0
                    if time.time() - ts > 300: continue
                    if 'message' not in event and 'postback' not in event: continue
                    is_echo = event.get('message', {}).get('is_echo', False)
                    if is_echo:
                        txt = event['message'].get('text', '').strip()
                        if txt.startswith('/ban') or txt.startswith('/unban'): asyncio.create_task(process_admin_echo(event.get('recipient', {}).get('id'), txt))
                        continue 
                    await TASK_QUEUE.put((sender_id, event))
    except Exception as e: print(f"Webhook Error: {e}", flush=True)
    return web.Response(text="EVENT_RECEIVED", status=200)

async def init_app():
    global GLOBAL_SESSION
    print("🚀 [Boykta FB V10.0 Gaming Edition] Booting up...", flush=True)
    connector = aiohttp.TCPConnector(limit=0)
    GLOBAL_SESSION = aiohttp.ClientSession(connector=connector)
    await init_db()
    asyncio.create_task(background_tasks())
    app = web.Application()
    app.add_routes(routes)
    app.on_startup.append(start_workers)
    return app

if __name__ == '__main__':
    start_cloudflared_token()
    web.run_app(init_app(), host='0.0.0.0', port=PORT)
