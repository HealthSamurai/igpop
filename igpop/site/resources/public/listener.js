//* Loop through all dropdown buttons to toggle between hiding and showing its dropdown content - This allows the user to have multiple dropdowns without any conflict */
var dropdown = document.getElementsByClassName("dropdown-btn");

function makeMeActive(thisElem) {
    var svg = thisElem.getElementsByTagName('svg')[0];
    thisElem.classList.toggle("active");
    var dropdownContent = thisElem.nextElementSibling;
    if (dropdownContent.style.display === "block") {
        /*if (svg === null) {
          alert('SVG does not exist!');
          } else {*/
        svg.setAttribute("transform", "rotate(-90)");
        dropdownContent.style.display = "none";
    } else {
        svg.setAttribute("transform", "rotate(0)");
        dropdownContent.style.display = "block";
    }
}

for (let i = 0; i < dropdown.length; i++) {
    dropdown[i].addEventListener("click", function() {
        makeMeActive(this, dropdown[i]);
    });
}

document.getElementsByClassName("whole-content-body")[0].style.height = window.innerHeight - 100; //document.getElementsByClassName("fhir-image")[0].style.height;
document.getElementsByClassName("whole-content-body")[0].style.top = window.top;
