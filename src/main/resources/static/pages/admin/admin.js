// Admin JavaScript
console.log('AT-Board Admin loaded');

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

// Form validation
document.addEventListener('DOMContentLoaded', function() {
    const forms = document.querySelectorAll('form');
    
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const requiredInputs = form.querySelectorAll('[required]');
            let isValid = true;
            
            requiredInputs.forEach(input => {
                if (!input.value.trim()) {
                    isValid = false;
                    input.style.borderColor = '#e74c3c';
                } else {
                    input.style.borderColor = '#ddd';
                }
            });
            
            if (!isValid) {
                e.preventDefault();
                showAlert('필수 항목을 모두 입력해주세요.');
            }
        });
    });
});
