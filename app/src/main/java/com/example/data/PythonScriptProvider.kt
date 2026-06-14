package com.example.data

object PythonScriptProvider {
    val ARABIC_PYTHON_SCRIPT = """
# -*- coding: encoding: utf-8 -*-
import os
import asyncio
import numpy as np
import ccxt.pro as ccxt  # استخدام ccxt.pro للعمليات غير المتزامنة والسرعة العالية
from dotenv import load_dotenv

# تحميل متغيرات البيئة من ملف .env
load_dotenv()

# قائمة العملات الرقمية للمراقبة والتداول (صيغة الفيوتشرز المستمرة في CCXT)
SYMBOLS = [
    'BTC/USDT:USDT',
    'ETH/USDT:USDT',
    'SOL/USDT:USDT',
    'XRP/USDT:USDT'
]

# الإعدادات الافتراضية للبوت
TIMEFRAME = '1m'          # فريم الدقيقة للسكالبينغ السريع
LEVERAGE = 10             # الرافعة المالية (10x)
MARGIN_MODE = 'ISOLATED'  # نوع الهامش: معزول (Isolated) لتقليل المخاطر المتقاطعة
POSITION_SIZE_PCT = 0.05  # استخدام 5% من الهامش المتاح لكل صفقة
TAKE_PROFIT_PCT = 1.5     # نسبة جني الأرباح (1.5%)
STOP_LOSS_PCT = 0.8       # نسبة وقف الخسارة (0.8%)

# فترات المؤشرات التقنية
RSI_PERIOD = 14
RSI_OVERSOLD = 30.0
RSI_OVERBOUGHT = 70.0
EMA_SHORT_PERIOD = 9
EMA_LONG_PERIOD = 21

async def init_exchange():
    '''
    تهيئة الاتصال بمنصة MEXC وتثبيت الإعدادات الأساسية
    '''
    api_key = os.getenv('MEXC_API_KEY')
    api_secret = os.getenv('MEXC_API_SECRET')
    
    if not api_key or not api_secret:
        raise ValueError("خطأ: لم يتم العثور على MEXC_API_KEY أو MEXC_API_SECRET في ملف .env!")

    # إنشاء كائن الاتصال غير المتزامن للمنصة
    exchange = ccxt.mexc({
        'apiKey': api_key,
        'secret': api_secret,
        'enableRateLimit': True,
        'options': {
            'defaultType': 'swap',  # تفعيل وضع العقود الآجلة المستمرة Perpetual/Swap
            'adjustForExchangeFee': True
        }
    })
    
    # تحميل الأسواق للتأكد من الاتصال
    await exchange.load_markets()
    print("تم الاتصال بمنصة MEXC بنجاح وتحميل أسواق الفيوتشرز.")
    return exchange

async def set_leverage_and_margin(exchange, symbol):
    '''
    ضبط الرافعة المالية ونوع الهامش لكل عملة تلقائياً قبل بدء التداول
    '''
    try:
        # 1. ضبط نوع الهامش (Isolated أو Cross)
        # نقوم أولاً بالاستفسار أو الإعداد مباشرة، منصات عديدة تدعم التعديل فقط عند عدم وجود مراكز مفتوحة
        try:
            await exchange.set_margin_mode(MARGIN_MODE, symbol)
            print(f"[{symbol}] تم ضبط نوع الهامش إلى: {MARGIN_MODE}")
        except Exception as e:
            # بعض الأوقات تكون المنصة مضبوطة بالفعل على هذا الخيار وتلقي خطأ عند إعادة الضبط
            print(f"[{symbol}] تنبيه أثناء ضبط الهامش (قد يكون مضبوطاً بالفعل): {e}")

        # 2. ضبط الرافعة المالية
        await exchange.set_leverage(LEVERAGE, symbol, {'type': 'swap'})
        print(f"[{symbol}] تم ضبط الرافعة المالية إلى: {LEVERAGE}x")

    except Exception as e:
        print(f"[{symbol}] خطأ أثناء ضبط الرافعة أو الهامش: {e}")

# حساب المتوسط المتحرك الأسي (EMA) يدوياً بدون مكتبات ثقيلة
def calculate_ema(prices, period):
    if len(prices) < period:
        return None
    alpha = 2.0 / (period + 1.0)
    ema = [prices[0]]
    for price in prices[1:]:
        ema.append(price * alpha + ema[-1] * (1.0 - alpha))
    return ema[-1]

# حساب مؤشر القوة النسبية (RSI) يدوياً وبدقة عالية
def calculate_rsi(prices, period=14):
    if len(prices) < period + 1:
        return None
    
    deltas = np.diff(prices)
    gains = np.where(deltas > 0, deltas, 0)
    losses = np.where(deltas < 0, -deltas, 0)
    
    # حساب المتوسط الأول للأرباح والخسائر
    avg_gain = np.mean(gains[:period])
    avg_loss = np.mean(losses[:period])
    
    if avg_loss == 0:
        return 100.0 if avg_gain > 0 else 50.0
        
    for i in range(period, len(deltas)):
        avg_gain = (avg_gain * (period - 1) + gains[i]) / period
        avg_loss = (avg_loss * (period - 1) + losses[i]) / period
        
    if avg_loss == 0:
        return 100.0
        
    rs = avg_gain / avg_loss
    return 100.0 - (100.0 / (1.0 + rs))

async def fetch_indicators(exchange, symbol):
    '''
    جلب الشموع وحساب المؤشرات التقنية (EMA 9, EMA 21, RSI 14)
    '''
    try:
        # جلب أحدث 50 شمعة
        ohlcv = await exchange.fetch_ohlcv(symbol, timeframe=TIMEFRAME, limit=50)
        if len(ohlcv) < 30:
            return None
            
        closes = [candle[4] for candle in ohlcv]  # أسعار الإغلاق للشموع
        current_price = closes[-1]
        
        # حساب المؤشرات
        ema_short = calculate_ema(closes, EMA_SHORT_PERIOD)
        ema_long = calculate_ema(closes, EMA_LONG_PERIOD)
        rsi = calculate_rsi(closes, RSI_PERIOD)
        
        return {
            'current_price': current_price,
            'ema_short': ema_short,
            'ema_long': ema_long,
            'rsi': rsi
        }
    except Exception as e:
        print(f"[{symbol}] خطأ أثناء جلب وتحليل البيانات الفنية: {e}")
        return None

async def check_active_positions(exchange, symbol):
    '''
    التحقق من وجود مركز مفتوح للعملة لتجنب الافتتاح المتكرر العشوائي
    '''
    try:
        # جلب كافة المراكز الحالية للعملة المحددة
        positions = await exchange.fetch_positions(symbols=[symbol])
        
        # تصفية المراكز المفتوحة فعلياً (حيث حجم العقد أكبر من صفر)
        active_positions = []
        for pos in positions:
            # حجم العقد في CCXT للفيوتشرز يكون في حقل 'contracts' أو 'contractSize'
            contracts = float(pos.get('contracts', 0) or 0)
            if contracts > 0:
                # الكود يحتاج معرفة الاتجاه (long أو short)
                side = pos.get('side', '').upper() # LONG or SHORT
                active_positions.append({
                    'side': side,
                    'contracts': contracts,
                    'entryPrice': float(pos.get('entryPrice') or 0),
                    'unrealizedPnl': float(pos.get('unrealizedPnl') or 0),
                    'pos': pos
                })
        return active_positions
    except Exception as e:
        print(f"[{symbol}] خطأ أثناء فحص المراكز المفتوحة: {e}")
        return []

async def open_position(exchange, symbol, side, current_price, available_balance):
    '''
    فتح مركز شراء (LONG) أو بيع (SHORT) بناءً على الإشارات
    '''
    try:
        # حساب حجم الصفقة بالدولار بناءً على نسبة المحفظة والرافعة المالية
        # الحجم الاسمي (Nominal Position Size) = الهامش المخصص * الرافعة المالية
        margin_allocated = available_balance * POSITION_SIZE_PCT
        position_value_usdt = margin_allocated * LEVERAGE
        
        # الكمية المطلوبة بالعملة الأساسية (Quantity = الحجم الاسمي / السعر الحالي)
        quantity = position_value_usdt / current_price
        
        # جلب حجم العقد الأدنى وتقريب الكمية لتوافق متطلبات العقد بالمنصة
        market = exchange.market(symbol)
        min_qty = market['limits']['amount']['min']
        
        # تقريب حجم الصفقة بناءً على دقة المنصة (Precision amount)
        quantity = exchange.amount_to_precision(symbol, quantity)
        quantity_float = float(quantity)
        
        if quantity_float < min_qty:
            print(f"[{symbol}] حجم الصفقة المحسوب ({quantity_float}) أقل من الحد الأدنى المقبول ({min_qty}).")
            return

        print(f"⚠️ [{symbol}] جاري فتح صفقة {side} بمعدل رافعة {LEVERAGE}x...")
        
        # صياغة بارامترات التحوط لـ MEXC Futures (مهم جداً تحديد اتجاه الهيدج)
        # لمعاملات الهيدج (Hedge Mode)، نقوم بإدخال حقل positionSide لتصويب الأمر إلى LONG أو SHORT
        params = {
            'positionSide': side,  # 'LONG' أو 'SHORT'
        }
        
        # نوع الأمر لفتح المركز (شراء لفتح لونغ، أو بيع لفتح شورت)
        order_side = 'buy' if side == 'LONG' else 'sell'
        
        # تنفيذ أمر السوق الفوري (Market Order)
        order = await exchange.create_order(
            symbol=symbol,
            type='market',
            side=order_side,
            amount=quantity_float,
            params=params
        )
        print(f"✅ [{symbol}] تم فتح مركز {side} بنجاح! كود العملية ID: {order['id']}")
        
    except Exception as e:
        print(f"❌ [{symbol}] فشل فتح مركز {side}: {e}")

async def close_position(exchange, symbol, side, quantity, current_price):
    '''
    إغلاق مركز مفتوح بالكامل عند الوصول للأهداف أو إشارات عكسية
    '''
    try:
        # في وضع الهيدج لإغلاق لونغ: نقوم ببيع عقد الـ 'LONG'
        # لإغلاق شورت: نقوم بشراء عقد الـ 'SHORT'
        order_side = 'sell' if side == 'LONG' else 'buy'
        params = {
            'positionSide': side,  # الإغلاق يستهدف نفس جانب المركز المفتوح
        }
        
        print(f"⚠️ [{symbol}] جاري إغلاق مركز {side} بالكامل بسعر السوق...")
        order = await exchange.create_order(
            symbol=symbol,
            type='market',
            side=order_side,
            amount=quantity,
            params=params
        )
        print(f"✅ [{symbol}] تم إغلاق مركز {side} بنجاح! الأرباح/الخسائر تمت تسويتها.")
    except Exception as e:
        print(f"❌ [{symbol}] فشل إغلاق مركز {symbol} {side}: {e}")

async def monitor_symbol(exchange, symbol):
    '''
    الحلقة البرمجية المستقلة لمراقبة وتحليل عملة واحدة بشكل متوازٍ
    '''
    print(f"🏁 بدء مراقبة العملة: {symbol}")
    
    # ضبط الإعدادات للعملة فور البدء
    await set_leverage_and_margin(exchange, symbol)
    
    while True:
        try:
            # 1. فحص هل يوجد صفقات قائمة حالياً لهذه العملة؟
            active_pos = await check_active_positions(exchange, symbol)
            
            # 2. الحصول على المؤشرات الفنية المحدثة
            indicators = await fetch_indicators(exchange, symbol)
            if not indicators:
                await asyncio.sleep(5)
                continue
                
            current_price = indicators['current_price']
            ema_short = indicators['ema_short']
            ema_long = indicators['ema_long']
            rsi = indicators['rsi']
            
            # 3. إدارة المراكز المفتوحة (Checking Stop Loss / Take Profit)
            if active_pos:
                # البوت لديه مركز مفتوح لهذه العملة، سنراقب أهداف الخروج
                for pos in active_pos:
                    side = pos['side']
                    entry_price = pos['entryPrice']
                    contracts = pos['contracts']
                    
                    # حساب نسبة التغير الحالية في السعر
                    price_change_pct = ((current_price - entry_price) / entry_price) * 100
                    
                    # عكس النسبة للشورت
                    actual_profit_pct = price_change_pct if side == 'LONG' else -price_change_pct
                    
                    # العائد الفعلي مع الرافعة المالية
                    leveraged_profit_pct = actual_profit_pct * LEVERAGE
                    
                    # التحقق من جني الأرباح (Take Profit)
                    if leveraged_profit_pct >= TAKE_PROFIT_PCT:
                        print(f"🎯 [{symbol}] تحقق هدف جني الأرباح (+{leveraged_profit_pct:.2f}%). جاري الإغلاق...")
                        await close_position(exchange, symbol, side, contracts, current_price)
                        
                    # التحقق من وقف الخسارة (Stop Loss)
                    elif leveraged_profit_pct <= -STOP_LOSS_PCT:
                        print(f"🛑 [{symbol}] تفعيل وقف الخسارة (-{leveraged_profit_pct:.2f}%). جاري الإغلاق...")
                        await close_position(exchange, symbol, side, contracts, current_price)
                        
                    # إشارة فنية عكسية كسبب إضافي للخروج (تقاطع EMA العكسي)
                    elif side == 'LONG' and ema_short < ema_long:
                        print(f"⚠️ [{symbol}] إشارة خروج فنية: تقاطع سلبي لـ EMA لصفقة LONG. جاري الإغلاق...")
                        await close_position(exchange, symbol, side, contracts, current_price)
                        
                    elif side == 'SHORT' and ema_short > ema_long:
                        print(f"⚠️ [{symbol}] إشارة خروج فنية: تقاطع إيجابي لـ EMA لصفقة SHORT. جاري الإغلاق...")
                        await close_position(exchange, symbol, side, contracts, current_price)
                        
            # 4. البحث عن فرص جديدة للدخول (No open positions)
            else:
                # التحقق من رصيد المحفظة المتاح لفتح الصفقات
                balance = await exchange.fetch_balance()
                free_balance = float(balance.get('free', {}).get('USDT', 0))
                
                # طباعة تقرير المراقبة الدوري
                print(f"🔍 [{symbol}] السعر: {current_price} | RSI: {rsi:.2f} | EMA_S: {ema_short:.2f} | EMA_L: {ema_long:.2f} | الرصيد المتاح: {free_balance:.2f} USDT")
                
                # شروط الدخول لصفقة شراء (LONG):
                # 1. مؤشر RSI في منطقة ذروة البيع (أقل من 30) وبدأ بالارتداد وتجاوز EMA القصير المتوسط الطويل
                # 2. تقاطع أسي ذهبي (EMA Short > EMA Long)
                if rsi < RSI_OVERSOLD and ema_short > ema_long:
                    print(f"🔥 إشارة دخول LONG قوية على {symbol}! | RSI: {rsi:.2f}")
                    await open_position(exchange, symbol, 'LONG', current_price, free_balance)
                    
                # شروط الدخول لصفقة بيع (SHORT):
                # 1. مؤشر RSI في منطقة ذروة الشراء (أعلى من 70) وبدأ بالتراجع
                # 2. تقاطع أسي سلبي (EMA Short < EMA Long)
                elif rsi > RSI_OVERBOUGHT and ema_short < ema_long:
                    print(f"🔥 إشارة دخول SHORT قوية على {symbol}! | RSI: {rsi:.2f}")
                    await open_position(exchange, symbol, 'SHORT', current_price, free_balance)

        except Exception as e:
            print(f"❌ [{symbol}] حدث خطأ غير متوقع في حلقة المراقبة: {e}")
            
        # الانتظار 2 ثانية قبل الفحص التالي (سرعة السكالبينغ الموزونة لمكافحة الحد المفروض للـ API)
        await asyncio.sleep(2)

async def main():
    '''
    الدالة الرئيسية لإطلاق البوت وإدارة المهام غير المتزامنة لكل العملات في نفس الوقت
    '''
    print("=" * 60)
    print("🚀 بدء تشغيل بوت سكالبينغ MEXC Perpetual Futures متعدد العملات")
    print("=" * 60)
    
    # تهيئة الاتصال بالمنصة
    try:
        exchange = await init_exchange()
    except Exception as e:
        print(f"❌ لم يتم تشغيل البوت بسبب فشل في الاتصال بالمنصة: {e}")
        return

    # تشغيل المهام المتوازية لجميع العملات المعرفة بـ asyncio
    tasks = []
    for symbol in SYMBOLS:
        tasks.append(monitor_symbol(exchange, symbol))
        
    # تشغيل جميع المهام بالتوازي
    try:
        await asyncio.gather(*tasks)
    except KeyboardInterrupt:
        print("\n🛑 تم إيقاف البوت بواسطة الاستجابة اليدوية للمستخدم.")
    finally:
        # إغلاق جلسة الاتصال بشكل آمن عند الخروج
        await exchange.close()
        print("🔌 تم إغلاق جلسة الاتصال بالمنصة بأمان. بوت التداول متوقف.")

if __name__ == '__main__':
    # تشغيل الحلقة البرمجية المستمرة غير المتزامنة
    asyncio.run(main())
""".trimIndent()
}
