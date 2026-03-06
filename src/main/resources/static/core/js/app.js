(function () {
    function on(el, event, selector, handler) {
        el.addEventListener(event, function (e) {
            var t = e.target;
            while (t && t !== el) {
                if (t.matches(selector)) return handler.call(t, e);
                t = t.parentElement;
            }
        });
    }

    window.App = window.App || {};
    window.App.on = on;
})();

// 공통 Alert 모달 (브라우저 alert 대체)
function showAlert(message) {
    var modal = document.getElementById('js-alert-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'js-alert-modal';
        modal.className = 'js-alert-modal';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.innerHTML =
            '<div class="js-alert-modal__backdrop"></div>' +
            '<div class="js-alert-modal__box">' +
            '<p class="js-alert-modal__msg"></p>' +
            '<button class="js-alert-modal__ok" type="button">확인</button>' +
            '</div>';
        document.body.appendChild(modal);
        function closeModal() { modal.classList.remove('js-alert-modal--open'); }
        modal.querySelector('.js-alert-modal__backdrop').addEventListener('click', closeModal);
        modal.querySelector('.js-alert-modal__ok').addEventListener('click', closeModal);
        document.addEventListener('keydown', function(e) {
            if ((e.key === 'Enter' || e.key === 'Escape') &&
                modal.classList.contains('js-alert-modal--open')) {
                closeModal();
            }
        });
    }
    modal.querySelector('.js-alert-modal__msg').textContent = message;
    modal.classList.add('js-alert-modal--open');
    modal.querySelector('.js-alert-modal__ok').focus();
}

// 공통 Confirm 모달 (브라우저 confirm 대체)
function showConfirm(message, onConfirm) {
    var modal = document.getElementById('js-confirm-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'js-confirm-modal';
        modal.className = 'js-confirm-modal';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.innerHTML =
            '<div class="js-confirm-modal__backdrop"></div>' +
            '<div class="js-confirm-modal__box">' +
            '<p class="js-confirm-modal__msg"></p>' +
            '<div class="js-confirm-modal__actions">' +
            '<button class="js-confirm-modal__cancel" type="button">취소</button>' +
            '<button class="js-confirm-modal__ok" type="button">확인</button>' +
            '</div></div>';
        document.body.appendChild(modal);
        function closeModal() { modal.classList.remove('js-confirm-modal--open'); }
        modal.querySelector('.js-confirm-modal__backdrop').addEventListener('click', closeModal);
        modal.querySelector('.js-confirm-modal__cancel').addEventListener('click', closeModal);
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && modal.classList.contains('js-confirm-modal--open')) {
                closeModal();
            }
        });
    }
    modal.querySelector('.js-confirm-modal__msg').textContent = message;
    // 이전 콜백 제거 후 새 콜백 등록
    var okBtn = modal.querySelector('.js-confirm-modal__ok');
    var newOk = okBtn.cloneNode(true);
    okBtn.parentNode.replaceChild(newOk, okBtn);
    newOk.addEventListener('click', function() {
        modal.classList.remove('js-confirm-modal--open');
        onConfirm();
    });
    modal.classList.add('js-confirm-modal--open');
    modal.querySelector('.js-confirm-modal__cancel').focus();
}
