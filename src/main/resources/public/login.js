(function () {
    const loginInput = document.getElementById('login');
    const passwordInput = document.getElementById('password');
    const submitBtn = document.getElementById('submitbtn');
    const errorMsg = document.getElementById('errorMsg');
    const warnMsg = document.getElementById('warnMsg');

    function xatoKorsat(matn) {
        errorMsg.textContent = matn || 'Xato username yoki parol!';
        errorMsg.style.display = 'block';
    }

    function xatoYashir() {
        errorMsg.style.display = 'none';
        warnMsg.style.display = 'none';
    }

    async function loginQil() {
        xatoYashir();

        const username = loginInput.value.trim();
        const password = passwordInput.value;

        if (!username || !password) {
            warnMsg.textContent = 'Username va parolni kiriting';
            warnMsg.style.display = 'block';
            return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = 'Kirilmoqda...';

        try {
            const res = await fetch('/admin/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: username, password: password })
            });

            let data = null;
            try { data = await res.json(); } catch (e) { /* bo'sh javob */ }

            if (res.ok && data && data.holat) {
                // Auth cookie server tomonidan o'rnatildi — dashboardga o'tamiz
                window.location.href = '/admin/dashboard';
            } else {
                xatoKorsat(data && data.message);
            }
        } catch (e) {
            xatoKorsat('Server bilan aloqa yo\u2018q. Qayta urinib ko\u2018ring.');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Kirish';
        }
    }

    submitBtn.addEventListener('click', loginQil);

    // Enter bosilganda ham login qilish
    [loginInput, passwordInput].forEach(function (el) {
        el.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') loginQil();
        });
    });
})();
