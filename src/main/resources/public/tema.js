/* =========================================================
   KUN / TUN REJIMI — boshqaruv
   Bu fayl <head> ichida, CSS'dan keyin ulanadi (defer YO'Q!) —
   shunda sahifa chizilishidan oldin rejim qo'yiladi va "oq chaqnash" bo'lmaydi.

   Foydalanish:  <button onclick="temaAlmash()">
   ========================================================= */
(function () {
    var KALIT = 'tema';

    function saqlangan() {
        try {
            return localStorage.getItem(KALIT);
        } catch (e) {
            return null;   // maxfiy rejimda localStorage bloklangan bo'lishi mumkin
        }
    }

    function tizimTemasi() {
        return window.matchMedia &&
            window.matchMedia('(prefers-color-scheme: dark)').matches ? 'tun' : 'kun';
    }

    function qoy(tema) {
        document.documentElement.setAttribute('data-tema', tema);
    }

    // 1) Sahifa chizilishidan oldin darhol qo'yamiz
    qoy(saqlangan() || tizimTemasi());

    // 2) Almashtirish
    window.temaAlmash = function () {
        var hozir = document.documentElement.getAttribute('data-tema');
        var yangi = hozir === 'tun' ? 'kun' : 'tun';

        // silliq o'tish uchun vaqtincha klass
        document.documentElement.classList.add('tema-almashuv');
        qoy(yangi);
        setTimeout(function () {
            document.documentElement.classList.remove('tema-almashuv');
        }, 300);

        try {
            localStorage.setItem(KALIT, yangi);
        } catch (e) { /* saqlanmasa ham rejim shu sahifada ishlaydi */ }

        return yangi;
    };

    window.temaHozirgi = function () {
        return document.documentElement.getAttribute('data-tema');
    };

    // 3) Boshqa tab'da almashtirilsa — bu tab ham ergashadi
    window.addEventListener('storage', function (e) {
        if (e.key === KALIT && e.newValue) qoy(e.newValue);
    });
})();
